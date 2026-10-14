package com.glowora.security;

import com.glowora.entity.Customer;
import com.glowora.exception.ForbiddenAccessException;
import com.glowora.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityOwnershipValidator {

    private final CustomerRepository customerRepository;

    /**
     * Returns the currently authenticated customer ID if logged in, or null if the request is unauthenticated.
     */
    public Long getAuthenticatedCustomerIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomerUserDetails userDetails) {
            return userDetails.getId();
        }
        String username = auth.getName();
        if (username != null && !username.isBlank()) {
            return customerRepository.findByEmailIgnoreCase(username)
                    .map(Customer::getId)
                    .orElse(null);
        }
        return null;
    }

    public boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Enforces ownership checks for customer-specific resources whenever a customer is authenticated.
     * Prevents an authenticated user from viewing or modifying another customer's profile, wishlist, cart, or orders
     * by tampering with an ID in the URL or request body. Administrators (ROLE_ADMIN) are permitted.
     */
    public void validateOwnership(Long targetCustomerId) {
        if (targetCustomerId == null || isCurrentUserAdmin()) {
            return;
        }
        Long authenticatedId = getAuthenticatedCustomerIdOrNull();
        if (authenticatedId != null && !authenticatedId.equals(targetCustomerId)) {
            throw new ForbiddenAccessException(
                    "Access denied: You are not authorized to access or modify another customer's data.");
        }
    }
}

