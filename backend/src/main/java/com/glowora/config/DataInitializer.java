package com.glowora.config;

import com.glowora.entity.Category;
import com.glowora.entity.Customer;
import com.glowora.entity.Product;
import com.glowora.entity.ProductReview;
import com.glowora.repository.CategoryRepository;
import com.glowora.repository.CustomerRepository;
import com.glowora.repository.ProductRepository;
import com.glowora.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${glowora.admin.name:Glowora Administrator}")
    private String adminName;

    @Value("${glowora.admin.email:admin@glowora.com}")
    private String adminEmail;

    @Value("${glowora.admin.password:Admin@Glowora2026}")
    private String adminPassword;

    @Value("${glowora.admin.phone:9800000001}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(String... args) {
        ensureInitialAdminAccount();

        if (categoryRepository.count() > 0 && productRepository.count() > 0) {
            upgradeExistingCatalogImagesToPhotorealistic();
            log.info("Glowora PostgreSQL catalog already initialized ({} categories, {} products).",
                    categoryRepository.count(), productRepository.count());
            return;
        }

        log.info("Seeding Glowora Beauty & Cosmetics catalog into PostgreSQL...");

        // 1. Seed all required Categories (5 parent groups + 20 specific categories)
        Map<String, Category> categories = new LinkedHashMap<>();

        // Parent group categories
        categories.put("Skincare", ensureCategory("Skincare", "Skincare",
                "Botanical-infused cleansers, serums, moisturizers, toners, and sun care for luminous skin.",
                "assets/images/categories/skincare.jpg"));
        categories.put("Haircare", ensureCategory("Haircare", "Haircare",
                "Nourishing shampoos, conditioners, cold-pressed hair oils, and shine-enhancing serums.",
                "assets/images/categories/haircare.jpg"));
        categories.put("Makeup", ensureCategory("Makeup", "Makeup",
                "Velvet lipsticks, breathable foundations, setting sprays, and long-wear eye definition.",
                "assets/images/categories/makeup.jpg"));
        categories.put("Body Care", ensureCategory("Body Care", "Body Care",
                "Aromatic body washes, exfoliating rose scrubs, and whipped botanical body butters.",
                "assets/images/categories/bodycare.jpg"));
        categories.put("Beauty Essentials", ensureCategory("Beauty Essentials", "Beauty Essentials",
                "Rose quartz facial tools, artisan vegan makeup brushes, and luxury vanity accessories.",
                "assets/images/categories/essentials.jpg"));

        // Skincare sub-categories
        categories.put("Face Wash", ensureCategory("Face Wash", "Skincare",
                "Gentle pH-balanced foaming and gel cleansers that purify without stripping moisture.",
                "assets/images/categories/skincare.jpg"));
        categories.put("Moisturizer", ensureCategory("Moisturizer", "Skincare",
                "Barrier-strengthening creams and gel-creams for 72-hour dewy hydration.",
                "assets/images/categories/skincare.jpg"));
        categories.put("Sunscreen", ensureCategory("Sunscreen", "Skincare",
                "Broad-spectrum SPF 50 PA++++ invisible sun protection with zero white cast.",
                "assets/images/categories/skincare.jpg"));
        categories.put("Toner", ensureCategory("Toner", "Skincare",
                "Alcohol-free botanical hydrosols and pore-refining essence toners.",
                "assets/images/categories/skincare.jpg"));
        categories.put("Vitamin C Serum", ensureCategory("Vitamin C Serum", "Skincare",
                "Potent antioxidant brightening serums formulated with stabilized L-Ascorbic Acid.",
                "assets/images/categories/skincare.jpg"));

        // Haircare sub-categories
        categories.put("Shampoo", ensureCategory("Shampoo", "Haircare",
                "Sulfate-free reparative shampoos enriched with keratin and hibiscus extract.",
                "assets/images/categories/haircare.jpg"));
        categories.put("Conditioner", ensureCategory("Conditioner", "Haircare",
                "Silky detangling conditioners infused with Moroccan argan and camellia oil.",
                "assets/images/categories/haircare.jpg"));
        categories.put("Hair Oil", ensureCategory("Hair Oil", "Haircare",
                "Cold-pressed rosemary, bhringraj, and sweet almond scalp & hair elixirs.",
                "assets/images/categories/haircare.jpg"));
        categories.put("Hair Serum", ensureCategory("Hair Serum", "Haircare",
                "Weightless frizz-control and thermal-shield glass hair finishing serums.",
                "assets/images/categories/haircare.jpg"));

        // Makeup sub-categories
        categories.put("Lipstick", ensureCategory("Lipstick", "Makeup",
                "Richly pigmented satin-matte and velvet-creme lip colors infused with rosehip butter.",
                "assets/images/categories/makeup.jpg"));
        categories.put("Foundation", ensureCategory("Foundation", "Makeup",
                "Second-skin serum foundations with hyaluronic acid and 24-hour radiant finish.",
                "assets/images/categories/makeup.jpg"));
        categories.put("Compact Powder", ensureCategory("Compact Powder", "Makeup",
                "Micro-milled silk blurring compact powders for soft-focus oil control.",
                "assets/images/categories/makeup.jpg"));
        categories.put("Mascara", ensureCategory("Mascara", "Makeup",
                "Smudge-proof panoramic volume and lash-lifting botanical mascaras.",
                "assets/images/categories/makeup.jpg"));
        categories.put("Setting Spray", ensureCategory("Setting Spray", "Makeup",
                "Rosewater and niacinamide micro-mist setting sprays for 16-hour makeup lock.",
                "assets/images/categories/makeup.jpg"));

        // Body Care sub-categories
        categories.put("Body Wash", ensureCategory("Body Wash", "Body Care",
                "Luxurious sulfate-free shower gels scented with Damascus rose and white tea.",
                "assets/images/categories/bodycare.jpg"));
        categories.put("Body Scrub", ensureCategory("Body Scrub", "Body Care",
                "Fine pink Himalayan salt and crushed rose petal polishing body scrubs.",
                "assets/images/categories/bodycare.jpg"));
        categories.put("Body Butter", ensureCategory("Body Butter", "Body Care",
                "Whipped shea, cocoa, and mango butter souffles for velvety-soft skin.",
                "assets/images/categories/bodycare.jpg"));

        // Beauty Essentials sub-categories
        categories.put("Beauty Tools", ensureCategory("Beauty Tools", "Beauty Essentials",
                "Authentic rose quartz rollers, gua sha sculpting stones, and ice globes.",
                "assets/images/categories/essentials.jpg"));
        categories.put("Makeup Brushes", ensureCategory("Makeup Brushes", "Beauty Essentials",
                "Handcrafted ultra-plush synthetic pro artistry brush collections.",
                "assets/images/categories/essentials.jpg"));
        categories.put("Cosmetic Accessories", ensureCategory("Cosmetic Accessories", "Beauty Essentials",
                "Velvet travel vanity pouches, microfiber blending sponges, and satin sleep masks.",
                "assets/images/categories/essentials.jpg"));

        if (productRepository.count() > 0) {
            upgradeExistingCatalogImagesToPhotorealistic();
            return;
        }

        // 2. Seed 20 signature Glowora products across all 20 subcategories
        List<Product> products = List.of(
                buildProduct(
                        "Rosewater & Amino Gentle Foaming Face Wash",
                        "A cloud-soft, soap-free daily cleanser formulated with Bulgarian Damascus Rosewater, Amino Acids, and Panthenol (Pro-Vitamin B5). Effortlessly dissolves impurities and sunscreen while leaving skin bouncy, calm, and hydrated.",
                        "GLOWORA Botanica",
                        categories.get("Face Wash"),
                        new BigDecimal("449.00"), new BigDecimal("599.00"), 45,
                        "assets/images/products/skincare/face-wash.jpg", 4.8, 3,
                        "Rosa Damascena Flower Water, Sodium Cocoyl Glycinate, Glycerin, Panthenol, Centella Asiatica Extract, Allantoin.",
                        "Massage 1-2 pumps onto damp face in gentle circular motions for 45 seconds. Rinse thoroughly with lukewarm water.",
                        "150 ml", true
                ),
                buildProduct(
                        "Ceramide & Rosehip Cloud Dew Moisturizer",
                        "An ultra-lightweight yet deeply restorative gel-cream powered by 5 Bio-Identical Ceramides, Cold-Pressed Rosehip Seed Oil, and Multi-Molecular Hyaluronic Acid for 72-hour glass-skin hydration.",
                        "GLOWORA Skin Lab",
                        categories.get("Moisturizer"),
                        new BigDecimal("699.00"), new BigDecimal("899.00"), 38,
                        "assets/images/products/skincare/moisturizer.jpg", 4.9, 2,
                        "Aqua, Ceramide NP, Ceramide AP, Ceramide EOP, Rosa Canina Fruit Oil, Sodium Hyaluronate, Squalane, Niacinamide.",
                        "Apply an even pearl-sized layer over cleansed and toned skin morning and night.",
                        "50 g", true
                ),
                buildProduct(
                        "Invisible Silk Sun Fluid SPF 50 PA++++",
                        "Weightless serum-textured broad-spectrum UVA/UVB sunscreen with encapsulated UV filters, Niacinamide, and White Peony extract. Absorbs in 5 seconds with zero white cast and a luminous satin-skin finish.",
                        "GLOWORA Sun Atelier",
                        categories.get("Sunscreen"),
                        new BigDecimal("599.00"), new BigDecimal("749.00"), 52,
                        "assets/images/products/skincare/sunscreen.jpg", 4.9, 2,
                        "Aqua, Bis-Ethylhexyloxyphenol Methoxyphenyl Triazine, Niacinamide, Paeonia Albiflora Root Extract, Tocopherol, Hyaluronic Acid.",
                        "Apply two finger-lengths generously onto face and neck 15 minutes before sun exposure. Reapply every 3-4 hours.",
                        "50 ml", true
                ),
                buildProduct(
                        "Damascus Rose & PHA Pore Refining Essence Toner",
                        "An alcohol-free hydrating mist-toner combining 78% Damascus Rose Hydrosol, 3% Gluconolactone (PHA), and Niacinamide to gently smooth skin texture, minimize pores, and prep skin for serums.",
                        "GLOWORA Botanica",
                        categories.get("Toner"),
                        new BigDecimal("499.00"), new BigDecimal("649.00"), 40,
                        "assets/images/products/skincare/toner.jpg", 4.7, 1,
                        "Rosa Damascena Flower Water, Gluconolactone, Niacinamide, Betaine, Sodium Hyaluronate, Chamomilla Recutita Extract.",
                        "Pat gently into clean skin with palms or sweep across the face using a cotton pad after cleansing.",
                        "200 ml", false
                ),
                buildProduct(
                        "15% L-Ascorbic + Ferulic Radiance Vitamin C Serum",
                        "Our bestselling clinical-grade brightening elixir featuring 15% stabilized 3-O-Ethyl Ascorbic Acid, Ferulic Acid, Vitamin E, and Kakadu Plum to visibly fade dark spots and boost collagen luminosity.",
                        "GLOWORA Skin Lab",
                        categories.get("Vitamin C Serum"),
                        new BigDecimal("799.00"), new BigDecimal("1099.00"), 60,
                        "assets/images/products/skincare/vitamin-c-serum.jpg", 4.9, 3,
                        "Aqua, 3-O-Ethyl Ascorbic Acid (15%), Ferulic Acid, Terminalia Ferdinandiana Fruit Extract, Tocopherol, Glutathione, Sodium Hyaluronate.",
                        "Dispense 3-4 drops onto clean, toned face every morning. Follow with moisturizer and SPF 50.",
                        "30 ml", true
                ),
                buildProduct(
                        "Hibiscus & Hydrolyzed Keratin Silk Repair Shampoo",
                        "A sulfate-free, salon-grade reparative shampoo enriched with Red Hibiscus Flower AHA, Hydrolyzed Plant Keratin, and Moroccan Argan Oil to strengthen brittle strands and restore mirror-like shine.",
                        "GLOWORA Hair Rituals",
                        categories.get("Shampoo"),
                        new BigDecimal("549.00"), new BigDecimal("699.00"), 34,
                        "assets/images/products/haircare/shampoo.jpg", 4.7, 1,
                        "Aqua, Sodium Lauroyl Methyl Isethionate, Hibiscus Sabdariffa Flower Extract, Hydrolyzed Wheat Protein, Argania Spinosa Kernel Oil, Biotin.",
                        "Emulsify between wet palms and massage into scalp for 60 seconds. Rinse thoroughly and follow with conditioner.",
                        "250 ml", false
                ),
                buildProduct(
                        "Camellia & Moroccan Argan Velvet Hair Conditioner",
                        "Rich detangling conditioner that wraps each hair fiber in Japanese Camellia Oil, Shea Butter, and Pro-Vitamin B5 to seal split ends and eliminate frizz without weighing hair down.",
                        "GLOWORA Hair Rituals",
                        categories.get("Conditioner"),
                        new BigDecimal("579.00"), new BigDecimal("729.00"), 30,
                        "assets/images/products/haircare/conditioner.jpg", 4.8, 1,
                        "Aqua, Cetearyl Alcohol, Behentrimonium Chloride, Camellia Japonica Seed Oil, Argania Spinosa Kernel Oil, Butyrospermum Parkii Butter.",
                        "Apply from mid-lengths to ends on freshly washed hair. Leave for 2-3 minutes before rinsing with cool water.",
                        "250 ml", false
                ),
                buildProduct(
                        "Rosemary & Bhringraj Cold-Pressed Scalp Revitalizing Hair Oil",
                        "A therapeutic 12-herb botanical hair elixir cold-pressed with Spanish Rosemary, Bhringraj, Amla, and Sweet Almond Oil to stimulate hair follicles, reduce hair fall, and nourish dry scalps.",
                        "GLOWORA Hair Rituals",
                        categories.get("Hair Oil"),
                        new BigDecimal("649.00"), new BigDecimal("849.00"), 42,
                        "assets/images/products/haircare/hair-oil.jpg", 4.9, 2,
                        "Prunus Amygdalus Dulcis Oil, Rosmarinus Officinalis Leaf Oil, Eclipta Prostrata Extract, Phyllanthus Emblica Extract, Simmondsia Chinensis Oil.",
                        "Part hair and apply 2 droppers directly onto scalp. Massage with fingertips for 5 minutes and leave for 2 hours or overnight.",
                        "100 ml", true
                ),
                buildProduct(
                        "24K Rose Gold Glass-Shine Anti-Frizz Hair Serum",
                        "Featherlight heat-protectant finishing serum infused with Rosehip Oil, Squalane, and Peptide Complex that shields hair up to 230°C and delivers instant frizz-free silkiness.",
                        "GLOWORA Hair Rituals",
                        categories.get("Hair Serum"),
                        new BigDecimal("629.00"), new BigDecimal("799.00"), 28,
                        "assets/images/products/haircare/hair-serum.jpg", 4.8, 1,
                        "Coco-Caprylate, Squalane, Rosa Canina Fruit Oil, Argania Spinosa Kernel Oil, Tocopheryl Acetate, Pisum Sativum Peptide.",
                        "Warm 1-2 pumps between palms and smooth over damp or dry mid-lengths and ends.",
                        "75 ml", false
                ),
                buildProduct(
                        "Velvet Couture Matte Lipstick - 04 Rosewood Muse",
                        "One-swipe haute pigment lipstick with a weightless cashmere-matte finish. Enriched with French Rose Wax, Hyaluronic Filling Spheres, and Jojoba Oil for 12-hour non-drying comfort.",
                        "GLOWORA Atelier Paris",
                        categories.get("Lipstick"),
                        new BigDecimal("699.00"), new BigDecimal("899.00"), 55,
                        "assets/images/products/makeup/lipstick.jpg", 4.9, 3,
                        "Dimethicone, Isoamyl Laurate, Rosa Centifolia Flower Wax, Simmondsia Chinensis Seed Oil, Sodium Hyaluronate, CI 77491, CI 15850.",
                        "Glide directly from the bullet starting at the Cupid's bow and sweep outward toward the corners.",
                        "4.2 g", true
                ),
                buildProduct(
                        "Luminous Silk Serum Foundation SPF 30 - Warm Ivory",
                        "Breathable medium-to-buildable serum foundation infused with 2% Niacinamide, Squalane, and light-diffusing pearl pigments for a natural lit-from-within complexion that lasts 24 hours.",
                        "GLOWORA Atelier Paris",
                        categories.get("Foundation"),
                        new BigDecimal("1199.00"), new BigDecimal("1499.00"), 25,
                        "assets/images/products/makeup/foundation.jpg", 4.8, 2,
                        "Aqua, Caprylyl Methicone, Niacinamide, Squalane, Glycerin, Titanium Dioxide, Iron Oxides, Sodium Hyaluronate.",
                        "Shake well. Dot 1-2 pumps across forehead, cheeks, nose, and chin, then buff seamlessly with a damp sponge or brush.",
                        "30 ml", true
                ),
                buildProduct(
                        "Airbrush Silk Blurring Compact Powder - Translucent Rose",
                        "Ultra-fine micro-milled setting compact infused with Rose Petal Powder and Silk Amino Acids. Instantly blurs pores, absorbs excess shine, and sets makeup without caking.",
                        "GLOWORA Atelier Paris",
                        categories.get("Compact Powder"),
                        new BigDecimal("749.00"), new BigDecimal("949.00"), 32,
                        "assets/images/products/makeup/compact-powder.jpg", 4.7, 1,
                        "Mica, Silica, Boron Nitride, Squalane, Rosa Gallica Flower Powder, Silk Amino Acids, Tocopherol.",
                        "Press the included velvet puff onto the T-zone and under-eyes to lock makeup in place.",
                        "9 g", false
                ),
                buildProduct(
                        "Sky-High Lash Lift & Panorama Volume Mascara",
                        "Jet-black tubing and volumizing mascara featuring a hourglass elastomer wand, Bamboo Extract, and Castor Seed Oil to lift, curl, and fan out every lash for 18 hours without flaking.",
                        "GLOWORA Atelier Paris",
                        categories.get("Mascara"),
                        new BigDecimal("599.00"), new BigDecimal("799.00"), 46,
                        "assets/images/products/makeup/mascara.jpg", 4.8, 2,
                        "Aqua, Copernicia Cerifera Cera, Ricinus Communis Seed Oil, Bambusa Vulgaris Extract, Panthenol, Iron Oxides (CI 77499).",
                        "Wiggle the wand at the base of lashes and sweep upward to the tips. Apply a second coat for dramatic volume.",
                        "9.5 ml", false
                ),
                buildProduct(
                        "Dewy Rose & Niacinamide 16H Makeup Setting Spray",
                        "Ultra-fine continuous cloud mist that melts powder layers into a seamless, skin-like finish while locking makeup against humidity and transfer for up to 16 hours.",
                        "GLOWORA Atelier Paris",
                        categories.get("Setting Spray"),
                        new BigDecimal("649.00"), new BigDecimal("849.00"), 37,
                        "assets/images/products/makeup/setting-spray.jpg", 4.8, 1,
                        "Aqua, Rosa Damascena Flower Water, Niacinamide, PVP, Propanediol, Aloe Barbadensis Leaf Juice, Panthenol.",
                        "Hold bottle 8-10 inches from face, close eyes, and mist in an 'X' and 'T' motion after completing makeup.",
                        "100 ml", false
                ),
                buildProduct(
                        "Damascus Rose & White Tea Luxury Shower Nectar Body Wash",
                        "Silky pH-balanced botanical body wash that transforms into a fragrant, creamy lather infused with French Rose Extract, White Tea Antioxidants, and Sweet Almond Milk.",
                        "GLOWORA Bath & Body",
                        categories.get("Body Wash"),
                        new BigDecimal("499.00"), new BigDecimal("649.00"), 44,
                        "assets/images/products/bodycare/body-wash.jpg", 4.7, 1,
                        "Aqua, Coco-Glucoside, Glycerin, Rosa Damascena Flower Extract, Camellia Sinensis Leaf Extract, Prunus Amygdalus Dulcis Oil.",
                        "Massage onto wet skin using hands or a loofah to create a rich foam, then rinse clean.",
                        "300 ml", false
                ),
                buildProduct(
                        "Pink Himalayan Salt & Crushed Rose Petal Polishing Body Scrub",
                        "Exfoliating spa body polish crafted with mineral-rich Pink Himalayan Salt, Organic Cane Sugar, Crushed Rose Petals, and Cold-Pressed Macadamia Oil for baby-soft, glowing skin.",
                        "GLOWORA Bath & Body",
                        categories.get("Body Scrub"),
                        new BigDecimal("679.00"), new BigDecimal("899.00"), 29,
                        "assets/images/products/bodycare/body-scrub.jpg", 4.9, 2,
                        "Sucrose, Sodium Chloride (Himalayan Pink Salt), Macadamia Ternifolia Seed Oil, Butyrospermum Parkii Butter, Rosa Centifolia Petals.",
                        "Scoop a generous amount and buff onto damp skin in circular motions, focusing on elbows, knees, and heels. Rinse warm.",
                        "250 g", true
                ),
                buildProduct(
                        "Whipped Shea & Rose Gold Souffle Body Butter",
                        "Decadent, non-greasy whipped body butter blending Raw Ghana Shea Butter, Murumuru Butter, Rosehip Oil, and subtle illuminating champagne micro-pearls.",
                        "GLOWORA Bath & Body",
                        categories.get("Body Butter"),
                        new BigDecimal("729.00"), new BigDecimal("949.00"), 33,
                        "assets/images/products/bodycare/body-butter.jpg", 4.8, 1,
                        "Aqua, Butyrospermum Parkii (Shea) Butter, Astrocaryum Murumuru Seed Butter, Rosa Canina Fruit Oil, Tocopherol, Mica.",
                        "Smooth generously over towel-dried skin after showering to lock in 48-hour moisture and subtle radiance.",
                        "200 g", false
                ),
                buildProduct(
                        "Authentic Brazilian Rose Quartz Facial Roller & Gua Sha Set",
                        "Hand-carved 100% natural Grade-A Brazilian Rose Quartz dual-ended facial roller and sculpting Gua Sha stone designed to boost lymphatic drainage, de-puff under-eyes, and contour facial muscles.",
                        "GLOWORA Luxe Tools",
                        categories.get("Beauty Tools"),
                        new BigDecimal("1299.00"), new BigDecimal("1799.00"), 22,
                        "assets/images/products/beauty-essentials/beauty-tools.jpg", 4.9, 2,
                        "100% Authentic Natural Brazilian Rose Quartz Crystal with Rose-Gold Electroplated Noise-Free Zinc Alloy Frame.",
                        "Apply 3-4 drops of facial serum or oil, then glide the roller and Gua Sha upward and outward along jawline, cheekbones, and brow bone.",
                        "2-Piece Gift Box", true
                ),
                buildProduct(
                        "Rose Gold Pro Artistry 8-Piece Vegan Makeup Brush Set",
                        "Professional cruelty-free brush collection featuring ultra-dense Kashmir-soft synthetic Taklon bristles, weighted rose-gold ferrules, and blush lacquered wooden handles in a travel cylinder.",
                        "GLOWORA Luxe Tools",
                        categories.get("Makeup Brushes"),
                        new BigDecimal("1499.00"), new BigDecimal("1999.00"), 19,
                        "assets/images/products/beauty-essentials/makeup-brushes.jpg", 4.9, 2,
                        "Cruelty-Free Micro-Crystal Synthetic Taklon Bristles, Rose-Gold Aluminum Ferrules, FSC-Certified Birch Handles.",
                        "Use the face brushes for foundation, powder, blush, and contour, and precision brushes for eyeshadow blending and liner.",
                        "8 Brushes + Case", false
                ),
                buildProduct(
                        "Quilted Blush Velvet Vanity Case & Microfiber Sponge Duo",
                        "Water-resistant quilted blush-pink velvet cosmetic organizer paired with two latex-free hydro-velvet makeup blending sponges for streak-free complexion perfection.",
                        "GLOWORA Luxe Tools",
                        categories.get("Cosmetic Accessories"),
                        new BigDecimal("849.00"), new BigDecimal("1149.00"), 36,
                        "assets/images/products/beauty-essentials/cosmetic-accessories.jpg", 4.8, 1,
                        "Quilted Plush Velvet Exterior, Wipe-Clean Satin Interior, Gold-Tone Dual Zippers, 2 Hydrophilic Non-Latex Blending Sponges.",
                        "Dampen blending sponge with water and squeeze out excess before bouncing foundation or concealer across skin.",
                        "Vanity Bag + 2 Sponges", false
                )
        );

        List<Product> savedProducts = productRepository.saveAll(products);

        // 3. Seed realistic verified reviews for featured products
        if (reviewRepository.count() == 0 && !savedProducts.isEmpty()) {
            seedSampleReviews(savedProducts);
        }

        log.info("Glowora catalog seeding complete: {} categories and {} products saved in PostgreSQL.",
                categoryRepository.count(), productRepository.count());
    }

    private void upgradeExistingCatalogImagesToPhotorealistic() {
        Map<String, String> productMap = Map.ofEntries(
                Map.entry("face-wash", "assets/images/products/skincare/face-wash.jpg"),
                Map.entry("moisturizer", "assets/images/products/skincare/moisturizer.jpg"),
                Map.entry("sunscreen", "assets/images/products/skincare/sunscreen.jpg"),
                Map.entry("toner", "assets/images/products/skincare/toner.jpg"),
                Map.entry("vitamin-c-serum", "assets/images/products/skincare/vitamin-c-serum.jpg"),
                Map.entry("shampoo", "assets/images/products/haircare/shampoo.jpg"),
                Map.entry("conditioner", "assets/images/products/haircare/conditioner.jpg"),
                Map.entry("hair-oil", "assets/images/products/haircare/hair-oil.jpg"),
                Map.entry("hair-serum", "assets/images/products/haircare/hair-serum.jpg"),
                Map.entry("lipstick", "assets/images/products/makeup/lipstick.jpg"),
                Map.entry("foundation", "assets/images/products/makeup/foundation.jpg"),
                Map.entry("compact-powder", "assets/images/products/makeup/compact-powder.jpg"),
                Map.entry("mascara", "assets/images/products/makeup/mascara.jpg"),
                Map.entry("setting-spray", "assets/images/products/makeup/setting-spray.jpg"),
                Map.entry("body-wash", "assets/images/products/bodycare/body-wash.jpg"),
                Map.entry("body-scrub", "assets/images/products/bodycare/body-scrub.jpg"),
                Map.entry("body-butter", "assets/images/products/bodycare/body-butter.jpg"),
                Map.entry("beauty-tools", "assets/images/products/beauty-essentials/beauty-tools.jpg"),
                Map.entry("makeup-brushes", "assets/images/products/beauty-essentials/makeup-brushes.jpg"),
                Map.entry("cosmetic-accessories", "assets/images/products/beauty-essentials/cosmetic-accessories.jpg")
        );

        List<Product> allProducts = productRepository.findAll();
        boolean anyProductUpdated = false;
        for (Product p : allProducts) {
            String url = p.getImageUrl() != null ? p.getImageUrl() : "";
            for (Map.Entry<String, String> entry : productMap.entrySet()) {
                if (url.contains(entry.getKey()) && !url.equals(entry.getValue())) {
                    p.setImageUrl(entry.getValue());
                    anyProductUpdated = true;
                    break;
                }
            }
        }
        if (anyProductUpdated) {
            productRepository.saveAll(allProducts);
            log.info("Upgraded existing PostgreSQL product image paths to photorealistic studio photography.");
        }

        List<Category> allCategories = categoryRepository.findAll();
        boolean anyCatUpdated = false;
        for (Category c : allCategories) {
            String url = c.getImageUrl() != null ? c.getImageUrl() : "";
            if (url.endsWith(".svg")) {
                c.setImageUrl(url.substring(0, url.length() - 4) + ".jpg");
                anyCatUpdated = true;
            }
        }
        if (anyCatUpdated) {
            categoryRepository.saveAll(allCategories);
        }
    }

    private Category ensureCategory(String name, String parentGroup, String description, String imageUrl) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .slug(Category.toSlug(name))
                        .parentGroup(parentGroup)
                        .description(description)
                        .imageUrl(imageUrl)
                        .active(true)
                        .build()));
    }

    private Product buildProduct(String name, String description, String brand, Category category,
                                 BigDecimal price, BigDecimal originalPrice, int stockQuantity,
                                 String imageUrl, double rating, int reviewCount,
                                 String ingredients, String howToUse, String volumeOrWeight, boolean featured) {
        Product p = Product.builder()
                .name(name)
                .description(description)
                .brand(brand)
                .category(category)
                .price(price)
                .originalPrice(originalPrice)
                .stockQuantity(stockQuantity)
                .imageUrl(imageUrl)
                .rating(rating)
                .reviewCount(reviewCount)
                .ingredients(ingredients)
                .howToUse(howToUse)
                .volumeOrWeight(volumeOrWeight)
                .active(true)
                .featured(featured)
                .build();
        p.normalizePricingAndDefaults();
        return p;
    }

    private void seedSampleReviews(List<Product> savedProducts) {
        Product p0 = savedProducts.get(0);
        Product p4 = savedProducts.get(4);
        Product p9 = savedProducts.get(9);

        reviewRepository.saveAll(List.of(
                ProductReview.builder()
                        .product(p0)
                        .customerName("Ananya Sharma")
                        .rating(5)
                        .title("So gentle and smells like real fresh roses!")
                        .comment("My combination skin feels super clean without any tightness. The rosewater scent is natural and luxurious.")
                        .verifiedPurchase(true)
                        .build(),
                ProductReview.builder()
                        .product(p0)
                        .customerName("Meera Nair")
                        .rating(5)
                        .title("Holy grail morning cleanser")
                        .comment("Removes every trace of sunscreen and leaves a soft glow. Highly recommend for sensitive skin.")
                        .verifiedPurchase(true)
                        .build(),
                ProductReview.builder()
                        .product(p4)
                        .customerName("Riya Kapoor")
                        .rating(5)
                        .title("Visible glow in 2 weeks!")
                        .comment("Zero oxidation, absorbs like water, and faded my post-acne marks noticeably within 14 days.")
                        .verifiedPurchase(true)
                        .build(),
                ProductReview.builder()
                        .product(p9)
                        .customerName("Priya Deshmukh")
                        .rating(5)
                        .title("The most flattering rosewood shade")
                        .comment("Feels like velvet on the lips and stays put through coffee and lunch without drying out my lips.")
                        .verifiedPurchase(true)
                        .build()
        ));
    }

    private void ensureInitialAdminAccount() {
        String normalizedEmail = adminEmail.trim().toLowerCase();
        Optional<Customer> existingOpt = customerRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existingOpt.isEmpty()) {
            Customer admin = Customer.builder()
                    .fullName(adminName.trim())
                    .email(normalizedEmail)
                    .phone(adminPhone.trim())
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role("ROLE_ADMIN")
                    .build();
            customerRepository.save(admin);
            log.info("Initialized default Glowora ROLE_ADMIN account: {}", normalizedEmail);
        } else {
            Customer admin = existingOpt.get();
            boolean updated = false;
            if (!"ROLE_ADMIN".equals(admin.getRole())) {
                admin.setRole("ROLE_ADMIN");
                updated = true;
            }
            if (!passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                updated = true;
            }
            if (updated) {
                customerRepository.save(admin);
                log.info("Synchronized initial Glowora ROLE_ADMIN account credentials: {}", normalizedEmail);
            }
        }
    }
}

