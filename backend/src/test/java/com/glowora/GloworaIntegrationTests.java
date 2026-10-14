package com.glowora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glowora.dto.*;
import com.glowora.entity.*;
import com.glowora.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GloworaIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${glowora.admin.email:admin@glowora.com}")
    private String configuredAdminEmail;

    @Value("${glowora.admin.password:Admin@Glowora2026}")
    private String configuredAdminPassword;

    private static String uniqueEmail1;
    private static String uniqueEmail2;
    private static Long customerId1;
    private static Long customerId2;
    private static MockHttpSession customer1Session;
    private static MockHttpSession adminSession;
    private static Long testCategoryId;
    private static Long testProductId;

    @BeforeAll
    static void initTestIdentifiers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        uniqueEmail1 = "test.user1." + suffix + "@glowora.com";
        uniqueEmail2 = "test.user2." + suffix + "@glowora.com";
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("1. Registration validation rejects invalid inputs and mismatched passwords")
    void testRegistrationValidation() throws Exception {
        RegisterRequestDto mismatchDto = RegisterRequestDto.builder()
                .fullName("Aarohi Verma")
                .email(uniqueEmail1)
                .phone("9876543210")
                .password("Glowora@123")
                .confirmPassword("Different@123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        RegisterRequestDto invalidFieldsDto = RegisterRequestDto.builder()
                .fullName("A")
                .email("not-an-email")
                .phone("123")
                .password("123")
                .confirmPassword("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFieldsDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("2. Customer registration hashes password with BCrypt, sets ROLE_CUSTOMER, prevents duplicate email, and never exposes passwordHash")
    void testRegistrationAndPasswordHashing() throws Exception {
        RegisterRequestDto validDto = RegisterRequestDto.builder()
                .fullName("Aarohi Verma")
                .email(uniqueEmail1)
                .phone("9876543210")
                .password("Glowora@123")
                .confirmPassword("Glowora@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.customer.id").exists())
                .andExpect(jsonPath("$.customer.email").value(uniqueEmail1))
                .andExpect(jsonPath("$.customer.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.customer.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.customer.password").doesNotExist())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        customerId1 = json.get("customer").get("id").asLong();

        Customer savedCustomer = customerRepository.findById(customerId1).orElseThrow();
        assertThat(savedCustomer.getPasswordHash()).isNotEqualTo("Glowora@123");
        assertThat(savedCustomer.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Glowora@123", savedCustomer.getPasswordHash())).isTrue();

        // Duplicate email registration must fail with 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isBadRequest());

        // Register second customer for ownership isolation test
        RegisterRequestDto secondUserDto = RegisterRequestDto.builder()
                .fullName("Second Customer")
                .email(uniqueEmail2)
                .phone("9876543211")
                .password("Glowora@123")
                .confirmPassword("Glowora@123")
                .build();

        MvcResult result2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUserDto)))
                .andExpect(status().isCreated())
                .andReturn();
        customerId2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("customer").get("id").asLong();
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("3. Customer login succeeds with valid credentials, fails with invalid credentials, and enforces ownership")
    void testCustomerLoginAndOwnership() throws Exception {
        LoginRequestDto badLogin = LoginRequestDto.builder()
                .email(uniqueEmail1)
                .password("WrongPassword@999")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        LoginRequestDto goodLogin = LoginRequestDto.builder()
                .email(uniqueEmail1)
                .password("Glowora@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.customer.id").value(customerId1))
                .andExpect(jsonPath("$.customer.role").value("ROLE_CUSTOMER"))
                .andReturn();

        customer1Session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(customer1Session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(customer1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId1));

        // Ownership protection: Customer 1 cannot access Customer 2's profile or cart
        mockMvc.perform(get("/api/customers/" + customerId2).session(customer1Session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/cart/" + customerId2).session(customer1Session))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("4. Admin security: customer cannot log in as admin or call admin APIs; valid admin login succeeds")
    void testAdminSecurityAndAuthentication() throws Exception {
        // 4a. Unauthenticated user cannot call admin endpoints -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/customers"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // 4b. Ordinary customer cannot log in via /api/admin/login -> 403 Forbidden
        LoginRequestDto customerLoginAttemptOnAdmin = LoginRequestDto.builder()
                .email(uniqueEmail1)
                .password("Glowora@123")
                .build();

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerLoginAttemptOnAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());

        // 4c. Ordinary logged-in customer (ROLE_CUSTOMER) cannot call admin APIs -> 403 Forbidden
        mockMvc.perform(get("/api/admin/dashboard").session(customer1Session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/customers").session(customer1Session))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/products")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // 4d. Invalid admin password -> 401 Unauthorized
        LoginRequestDto badAdminLogin = LoginRequestDto.builder()
                .email(configuredAdminEmail)
                .password("WrongAdminPassword@000")
                .build();

        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badAdminLogin)))
                .andExpect(status().isUnauthorized());

        // 4e. Valid admin login -> 200 OK with ROLE_ADMIN and BCrypt hash in PostgreSQL
        Customer adminEntity = customerRepository.findByEmailIgnoreCase(configuredAdminEmail).orElseThrow();
        assertThat(adminEntity.getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(adminEntity.getPasswordHash()).startsWith("$2a$");

        LoginRequestDto validAdminLogin = LoginRequestDto.builder()
                .email(configuredAdminEmail)
                .password(configuredAdminPassword)
                .build();

        MvcResult adminLoginRes = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAdminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.customer.email").value(configuredAdminEmail.toLowerCase()))
                .andExpect(jsonPath("$.customer.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.customer.passwordHash").doesNotExist())
                .andReturn();

        adminSession = (MockHttpSession) adminLoginRes.getRequest().getSession(false);
        assertThat(adminSession).isNotNull();

        mockMvc.perform(get("/api/admin/me").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("5. Admin Category & Product CRUD and public Shop visibility")
    void testAdminCategoryAndProductCrud() throws Exception {
        String catName = "Luxury Elixir " + UUID.randomUUID().toString().substring(0, 5);
        CategoryRequestDto createCat = CategoryRequestDto.builder()
                .name(catName)
                .parentGroup("Skincare")
                .description("Limited edition botanical elixirs")
                .imageUrl("assets/images/categories/skincare.svg")
                .active(true)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/categories")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(catName))
                .andReturn();

        testCategoryId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        // Admin creates a new Product via POST /api/products
        ProductRequestDto createProd = ProductRequestDto.builder()
                .name("Golden Saffron Night Recovery Serum")
                .description("Intensive overnight brightening serum infused with Kashmiri saffron and squalane.")
                .brand("GLOWORA Botanica")
                .categoryId(testCategoryId)
                .price(new BigDecimal("850.00"))
                .originalPrice(new BigDecimal("1000.00"))
                .stockQuantity(10)
                .imageUrl("assets/images/products/vitamin-c-serum.svg")
                .rating(4.9)
                .volumeOrWeight("30 ml")
                .active(true)
                .featured(true)
                .build();

        MvcResult prodRes = mockMvc.perform(post("/api/products")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.discountPercentage").value(15))
                .andReturn();

        testProductId = objectMapper.readTree(prodRes.getResponse().getContentAsString()).get("id").asLong();

        // Admin updates Product price -> discount auto-recalculates to 20%
        createProd.setPrice(new BigDecimal("800.00"));
        mockMvc.perform(put("/api/products/" + testProductId)
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(800.00))
                .andExpect(jsonPath("$.discountPercentage").value(20));

        // Verify newly added product appears on public customer Shop APIs without authentication
        mockMvc.perform(get("/api/products/search").param("keyword", "Saffron"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testProductId));

        mockMvc.perform(get("/api/products/category/" + testCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testProductId));
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("6. Admin Customer directory, search, and customer details never expose password hashes")
    void testAdminCustomerManagement() throws Exception {
        // List all customers
        MvcResult listRes = mockMvc.perform(get("/api/admin/customers").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        String rawListJson = listRes.getResponse().getContentAsString();
        assertThat(rawListJson).doesNotContain("passwordHash");
        assertThat(rawListJson).contains(uniqueEmail1);

        // Search customer by email keyword
        mockMvc.perform(get("/api/admin/customers")
                        .session(adminSession)
                        .param("keyword", uniqueEmail1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(customerId1))
                .andExpect(jsonPath("$[0].fullName").value("Aarohi Verma"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());

        // View specific customer details
        mockMvc.perform(get("/api/admin/customers/" + customerId1).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId1))
                .andExpect(jsonPath("$.email").value(uniqueEmail1))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("7. Cart and Wishlist operations calculate totals and validate invalid quantities")
    void testCartAndWishlistOperations() throws Exception {
        WishlistItemRequestDto wishReq = WishlistItemRequestDto.builder().productId(testProductId).build();
        mockMvc.perform(post("/api/wishlist/" + customerId1 + "/items")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wishReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(delete("/api/wishlist/" + customerId1 + "/items/" + testProductId)
                        .session(customer1Session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));

        CartItemRequestDto invalidQty = CartItemRequestDto.builder()
                .productId(testProductId)
                .quantity(0)
                .build();
        mockMvc.perform(post("/api/cart/" + customerId1 + "/items")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQty)))
                .andExpect(status().isBadRequest());

        CartItemRequestDto validCartReq = CartItemRequestDto.builder()
                .productId(testProductId)
                .quantity(2)
                .build();
        mockMvc.perform(post("/api/cart/" + customerId1 + "/items")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCartReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(1600.00))
                .andExpect(jsonPath("$.discountAmount").value(400.00))
                .andExpect(jsonPath("$.shippingAmount").value(0))
                .andExpect(jsonPath("$.finalTotal").value(1600.00));
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("8. Insufficient inventory is rejected and rolls back without changing stock")
    void testInsufficientInventoryAndRollback() throws Exception {
        int stockBefore = productRepository.findById(testProductId).orElseThrow().getStockQuantity();
        assertThat(stockBefore).isEqualTo(10);

        OrderRequestDto excessiveOrder = OrderRequestDto.builder()
                .customerId(customerId1)
                .customerName("Aarohi Verma")
                .email(uniqueEmail1)
                .phone("9876543210")
                .deliveryAddress("42 Rose Quartz Boulevard, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560038")
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .items(List.of(OrderItemRequestDto.builder()
                        .productId(testProductId)
                        .quantity(50)
                        .build()))
                .build();

        mockMvc.perform(post("/api/orders")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveOrder)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        int stockAfter = productRepository.findById(testProductId).orElseThrow().getStockQuantity();
        assertThat(stockAfter).isEqualTo(10);
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("9. Order placement, Admin Dashboard summary cards, revenue rule (excludes PENDING/CANCELLED), and stock restoration")
    void testOrderPlacementDashboardRevenueRuleAndCancellation() throws Exception {
        // Record baseline revenue before placing order
        MvcResult dashBeforeRes = mockMvc.perform(get("/api/admin/dashboard").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").exists())
                .andExpect(jsonPath("$.totalCustomers").exists())
                .andExpect(jsonPath("$.totalOrders").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andReturn();

        BigDecimal revenueBefore = new BigDecimal(
                objectMapper.readTree(dashBeforeRes.getResponse().getContentAsString()).get("totalRevenue").asText()
        );

        // Place order using the 2 items in Customer 1's cart (finalTotal = 1600.00, orderStatus = CONFIRMED)
        OrderRequestDto validOrder = OrderRequestDto.builder()
                .customerId(customerId1)
                .customerName("Aarohi Verma")
                .email(uniqueEmail1)
                .phone("9876543210")
                .deliveryAddress("42 Rose Quartz Boulevard, Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560038")
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .orderNotes("Please leave at reception")
                .build();

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrder)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.finalTotal").value(1600.00))
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify inventory decreased from 10 to 8
        assertThat(productRepository.findById(testProductId).orElseThrow().getStockQuantity()).isEqualTo(8);

        // CONFIRMED order is included in completed revenue (+1600.00)
        MvcResult dashConfirmedRes = mockMvc.perform(get("/api/admin/dashboard").session(adminSession))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal revenueAfterConfirmed = new BigDecimal(
                objectMapper.readTree(dashConfirmedRes.getResponse().getContentAsString()).get("totalRevenue").asText()
        );
        assertThat(revenueAfterConfirmed).isEqualByComparingTo(revenueBefore.add(new BigDecimal("1600.00")));

        // Set order status to PENDING -> must be excluded from totalRevenue
        OrderStatusUpdateDto pendingDto = OrderStatusUpdateDto.builder()
                .orderStatus(OrderStatus.PENDING)
                .build();
        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pendingDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("PENDING"));

        MvcResult dashPendingRes = mockMvc.perform(get("/api/admin/dashboard").session(adminSession))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal revenueWhenPending = new BigDecimal(
                objectMapper.readTree(dashPendingRes.getResponse().getContentAsString()).get("totalRevenue").asText()
        );
        assertThat(revenueWhenPending).isEqualByComparingTo(revenueBefore);

        // Cancel order -> must remain excluded from totalRevenue and restore stock from 8 back to 10
        OrderStatusUpdateDto cancelDto = OrderStatusUpdateDto.builder()
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        mockMvc.perform(put("/api/orders/" + orderId + "/status")
                        .session(customer1Session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        MvcResult dashCancelledRes = mockMvc.perform(get("/api/admin/dashboard").session(adminSession))
                .andExpect(status().isOk())
                .andReturn();
        BigDecimal revenueWhenCancelled = new BigDecimal(
                objectMapper.readTree(dashCancelledRes.getResponse().getContentAsString()).get("totalRevenue").asText()
        );
        assertThat(revenueWhenCancelled).isEqualByComparingTo(revenueBefore);

        Product productAfterCancel = productRepository.findById(testProductId).orElseThrow();
        assertThat(productAfterCancel.getStockQuantity()).isEqualTo(10);

        // Clean up test order, product & category using adminSession
        orderRepository.deleteById(orderId);
        mockMvc.perform(delete("/api/products/" + testProductId).session(adminSession)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/categories/" + testCategoryId).session(adminSession)).andExpect(status().isOk());
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("10. Admin logout invalidates session and blocks subsequent admin API access")
    void testAdminLogoutPreventsFurtherAccess() throws Exception {
        mockMvc.perform(post("/api/admin/logout").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/dashboard").session(adminSession))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/customers").session(adminSession))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("11. Contact form validation and persistence in PostgreSQL")
    void testContactFormValidationAndPersistence() throws Exception {
        ContactRequestDto invalidContact = ContactRequestDto.builder()
                .fullName("Aarohi")
                .email("invalid-email")
                .subject("")
                .message("Hi")
                .build();

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidContact)))
                .andExpect(status().isBadRequest());

        ContactRequestDto validContact = ContactRequestDto.builder()
                .fullName("Aarohi Verma")
                .email(uniqueEmail1)
                .phone("9876543210")
                .subject("Skincare Routine Consultation")
                .message("Hello Glowora team, could you recommend a morning routine for dry sensitive skin?")
                .build();

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validContact)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }
}
