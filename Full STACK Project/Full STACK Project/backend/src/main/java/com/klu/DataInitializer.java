package com.klu;

import com.klu.model.*;
import com.klu.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final ReviewRepository reviewRepo;
    private final PromotionRepository promoRepo;
    private final IssueRepository issueRepo;
    private final PaymentRepository paymentRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepo, ProductRepository productRepo,
                           OrderRepository orderRepo, ReviewRepository reviewRepo,
                           PromotionRepository promoRepo, IssueRepository issueRepo,
                           PaymentRepository paymentRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.reviewRepo = reviewRepo;
        this.promoRepo = promoRepo;
        this.issueRepo = issueRepo;
        this.paymentRepo = paymentRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // Seed users
        userRepo.saveAll(List.of(
            User.builder().id("U001").name("Admin User").email("admin@tribalcraft.com")
                .password(encoder.encode("Admin@123")).role("admin").savedAddress("12 Market Road, Hyderabad, Telangana 500001").build(),
            User.builder().id("A001").name("Meena Dhamale").email("artisan@tribalcraft.com")
                .password(encoder.encode("Artisan@123")).role("artisan").build(),
            User.builder().id("A002").name("Rupa Hembrom").email("artisan2@tribalcraft.com")
                .password(encoder.encode("Artisan@123")).role("artisan").build(),
            User.builder().id("C001").name("Ravi Sharma").email("customer@tribalcraft.com")
                .password(encoder.encode("Customer@123")).role("customer")
                .savedAddress("221 Heritage Lane, Vizag, Andhra Pradesh 530016").build(),
            User.builder().id("CC001").name("Dr. Asha Kisku").email("consultant@tribalcraft.com")
                .password(encoder.encode("Consultant@123")).role("consultant").build()
        ));

        // Seed products
        productRepo.saveAll(List.of(
            Product.builder().id("P001").name("Warli Story Canvas").category("Painting").price(2499).stock(8)
                .artisanId("A001").artisanName("Meena Dhamale").region("Maharashtra")
                .description("Traditional Warli folk storytelling hand-painted on natural canvas.")
                .culturalNote("Warli painting reflects tribal rituals, farming life, and festivals.")
                .imageUrl("https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?auto=format&fit=crop&w=1200&q=80")
                .authenticityStatus("approved").rating(5.0).build(),
            Product.builder().id("P002").name("Bamboo Weave Basket Set").category("Home Decor").price(1799).stock(17)
                .artisanId("A002").artisanName("Rupa Hembrom").region("Jharkhand")
                .description("Eco-friendly bamboo basket set, hand-woven using indigenous techniques.")
                .culturalNote("Bamboo weaving is used in daily life and ceremonial gifting.")
                .imageUrl("https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=1200&q=80")
                .authenticityStatus("pending").rating(4.6).build(),
            Product.builder().id("P003").name("Dokra Brass Figurine").category("Sculpture").price(3299).stock(5)
                .artisanId("A003").artisanName("Gopinath Murmu").region("Chhattisgarh")
                .description("Lost-wax cast Dokra brass figurine with rich tribal motifs.")
                .culturalNote("Dokra metal craft dates back over 4,000 years in central India.")
                .imageUrl("https://images.unsplash.com/photo-1612196808214-b7e239e5f5ea?auto=format&fit=crop&w=1200&q=80")
                .authenticityStatus("approved").rating(4.9).build(),
            Product.builder().id("P004").name("Toda Embroidered Shawl").category("Textiles").price(2899).stock(13)
                .artisanId("A004").artisanName("Kala Devi").region("Tamil Nadu")
                .description("Hand-embroidered shawl inspired by Toda geometric motifs.")
                .culturalNote("Toda embroidery is known for red-black patterns and symbolic forms.")
                .imageUrl("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=1200&q=80")
                .authenticityStatus("review_requested").rating(4.7).build()
        ));

        // Seed promotions
        promoRepo.saveAll(List.of(
            Promotion.builder().id("PR001").title("Tribal Heritage Week").discountPercent(15).category("All").active(true).build(),
            Promotion.builder().id("PR002").title("Eco Craft Special").discountPercent(10).category("Home Decor").active(true).build()
        ));

        // Seed issues
        issueRepo.save(Issue.builder().id("I001").type("delivery").message("Order O1002 delayed by courier partner.").status("open").build());

        // Seed orders with items
        Order o1 = Order.builder().id("O1001").customerId("C001").customerName("Ravi Sharma").amount(2499)
            .shippingAddress("221 Heritage Lane, Vizag, Andhra Pradesh 530016").status("confirmed").createdAt("2026-02-18").build();
        OrderItem oi1 = OrderItem.builder().productId("P001").qty(1).price(2499).order(o1).build();
        o1.setItems(List.of(oi1));
        orderRepo.save(o1);

        Order o2 = Order.builder().id("O1002").customerId("C001").customerName("Ravi Sharma").amount(3299)
            .shippingAddress("221 Heritage Lane, Vizag, Andhra Pradesh 530016").status("shipped").createdAt("2026-02-20").build();
        OrderItem oi2 = OrderItem.builder().productId("P003").qty(1).price(3299).order(o2).build();
        o2.setItems(List.of(oi2));
        orderRepo.save(o2);

        // Seed payments
        paymentRepo.saveAll(List.of(
            Payment.builder().id("PAY1001").orderId("O1001").customerId("C001").customerName("Ravi Sharma")
                .amount(2499).method("upi").details("ravi@upi").status("success")
                .transactionRef("TXNAX8FD29Q").createdAt("2026-02-18").build(),
            Payment.builder().id("PAY1002").orderId("O1002").customerId("C001").customerName("Ravi Sharma")
                .amount(3299).method("cod").details("").status("pending")
                .transactionRef("TXNK7PE04DR").createdAt("2026-02-20").build()
        ));

        // Seed reviews
        reviewRepo.saveAll(List.of(
            Review.builder().id("R001").productId("P001").customerName("Ravi Sharma").rating(5)
                .comment("Excellent detailing and authentic tribal art.").createdAt("2026-02-19").build(),
            Review.builder().id("R002").productId("P003").customerName("Nisha Verma").rating(4)
                .comment("Beautiful craftsmanship, delivery was smooth.").createdAt("2026-02-21").build()
        ));

        System.out.println(">>> Database seeded with initial data");
    }
}
