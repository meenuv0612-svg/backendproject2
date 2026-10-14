package com.glowora.service.impl;

import com.glowora.dto.AuthResponseDto;
import com.glowora.dto.CustomerResponseDto;
import com.glowora.dto.LoginRequestDto;
import com.glowora.dto.RegisterRequestDto;
import com.glowora.entity.Cart;
import com.glowora.entity.Customer;
import com.glowora.entity.Wishlist;
import com.glowora.exception.BadRequestException;
import com.glowora.exception.DuplicateResourceException;
import com.glowora.exception.ForbiddenAccessException;
import com.glowora.exception.ResourceNotFoundException;
import com.glowora.exception.UnauthorizedException;
import com.glowora.mapper.EntityMapper;
import com.glowora.repository.CartRepository;
import com.glowora.repository.CustomerRepository;
import com.glowora.repository.WishlistRepository;
import com.glowora.security.CustomerUserDetails;
import com.glowora.security.GloworaTokenService;
import com.glowora.security.SecurityOwnershipValidator;
import com.glowora.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityOwnershipValidator ownershipValidator;
    private final GloworaTokenService tokenService;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto requestDto,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String normalizedEmail = requestDto.getEmail().trim().toLowerCase();

        if (!requestDto.getPassword().equals(requestDto.getConfirmPassword())) {
            throw new BadRequestException("Password and confirm password do not match.");
        }

        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email '" + normalizedEmail + "' is already registered.");
        }

        Customer customer = Customer.builder()
                .fullName(requestDto.getFullName().trim())
                .email(normalizedEmail)
                .phone(requestDto.getPhone().trim())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .role("ROLE_CUSTOMER")
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        Cart cart = Cart.builder().customer(savedCustomer).build();
        cartRepository.save(cart);

        Wishlist wishlist = Wishlist.builder().customer(savedCustomer).build();
        wishlistRepository.save(wishlist);

        // Authenticate and establish session security context
        CustomerUserDetails userDetails = new CustomerUserDetails(savedCustomer);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
        if (request != null && response != null) {
            securityContextRepository.saveContext(context, request, response);
        }

        String token = tokenService.generateToken(savedCustomer);
        if (response != null) {
            response.setHeader("Authorization", "Bearer " + token);
        }

        return AuthResponseDto.builder()
                .success(true)
                .message("Registration successful. Welcome to Glowora!")
                .token(token)
                .role(savedCustomer.getRole())
                .customer(entityMapper.toCustomerDto(savedCustomer))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto requestDto,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        String normalizedEmail = requestDto.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, requestDto.getPassword())
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (request != null && response != null) {
            securityContextRepository.saveContext(context, request, response);
        }

        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        String token = tokenService.generateToken(customer);
        if (response != null) {
            response.setHeader("Authorization", "Bearer " + token);
        }

        return AuthResponseDto.builder()
                .success(true)
                .message("Login successful. Welcome back, " + customer.getFullName() + "!")
                .token(token)
                .role(customer.getRole())
                .customer(entityMapper.toCustomerDto(customer))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto adminLogin(LoginRequestDto requestDto,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String normalizedEmail = requestDto.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, requestDto.getPassword())
        );

        Customer customer = customerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        if (!"ROLE_ADMIN".equals(customer.getRole())) {
            SecurityContextHolder.clearContext();
            if (request != null) {
                HttpSession existingSession = request.getSession(false);
                if (existingSession != null) {
                    existingSession.invalidate();
                }
            }
            throw new ForbiddenAccessException(
                    "Access denied: Ordinary customer accounts cannot log in to the Glowora Admin Portal.");
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (request != null && response != null) {
            securityContextRepository.saveContext(context, request, response);
        }

        String token = tokenService.generateToken(customer);
        if (response != null) {
            response.setHeader("Authorization", "Bearer " + token);
        }

        return AuthResponseDto.builder()
                .success(true)
                .message("Administrator login successful. Welcome, " + customer.getFullName() + "!")
                .token(token)
                .role(customer.getRole())
                .customer(entityMapper.toCustomerDto(customer))
                .build();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCurrentCustomer() {
        Long customerId = ownershipValidator.getAuthenticatedCustomerIdOrNull();
        if (customerId == null) {
            throw new UnauthorizedException("No authenticated customer session found. Please log in.");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated customer not found."));
        return entityMapper.toCustomerDto(customer);
    }
}

