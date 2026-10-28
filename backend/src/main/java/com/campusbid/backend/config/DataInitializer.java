package com.campusbid.backend.config;

import com.campusbid.backend.model.ItemCondition;
import com.campusbid.backend.model.Listing;
import com.campusbid.backend.model.ListingStatus;
import com.campusbid.backend.repository.ListingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(ListingRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                // Listing 1: Electronics
                Listing item1 = new Listing();
                item1.setTitle("MacBook Air M1 (16GB RAM, 256GB SSD) - Space Grey");
                item1.setCategory("Electronics");
                item1.setDescription("Excellent condition MacBook Air M1 used for 2 semesters. Battery health 91%. Comes with original 30W USB-C charger and protective sleeve.");
                item1.setCondition(ItemCondition.LIKE_NEW);
                item1.setStartingPrice(new BigDecimal("550.00"));
                item1.setMinBidIncrement(new BigDecimal("15.00"));
                item1.setCurrentPrice(new BigDecimal("580.00"));
                item1.setClosingTime(LocalDateTime.now().plusDays(3).plusHours(5));
                item1.setStatus(ListingStatus.ACTIVE);
                item1.setSellerId("student-101");
                item1.setSellerName("Alex Chen");
                item1.setSellerEmail("alex.chen@campus.edu");
                item1.setPhotoUrls(List.of(
                        "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?auto=format&fit=crop&w=800&q=80"
                ));
                repository.save(item1);

                // Listing 2: Textbooks
                Listing item2 = new Listing();
                item2.setTitle("Introduction to Algorithms (CLRS) 4th Edition");
                item2.setCategory("Textbooks");
                item2.setDescription("Required textbook for CS 301 Data Structures and Algorithms. Hardcover copy in good condition with minor highlighter marks in Chapter 2.");
                item2.setCondition(ItemCondition.GOOD);
                item2.setStartingPrice(new BigDecimal("45.00"));
                item2.setMinBidIncrement(new BigDecimal("5.00"));
                item2.setCurrentPrice(new BigDecimal("50.00"));
                item2.setClosingTime(LocalDateTime.now().plusDays(2).plusHours(12));
                item2.setStatus(ListingStatus.ACTIVE);
                item2.setSellerId("student-102");
                item2.setSellerName("Maya Patel");
                item2.setSellerEmail("maya.patel@campus.edu");
                item2.setPhotoUrls(List.of(
                        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=800&q=80"
                ));
                repository.save(item2);

                // Listing 3: Dorm & Furniture
                Listing item3 = new Listing();
                item3.setTitle("Compact Dorm Mini Fridge with Freezer Compartment");
                item3.setCategory("Dorm & Furniture");
                item3.setDescription("Energy Star certified 3.2 cu. ft. mini fridge. Clean and runs quietly, perfect for dorm rooms. Moving off campus so must sell!");
                item3.setCondition(ItemCondition.GOOD);
                item3.setStartingPrice(new BigDecimal("70.00"));
                item3.setMinBidIncrement(new BigDecimal("10.00"));
                item3.setCurrentPrice(new BigDecimal("70.00"));
                item3.setClosingTime(LocalDateTime.now().plusDays(4).plusHours(8));
                item3.setStatus(ListingStatus.ACTIVE);
                item3.setSellerId("student-101");
                item3.setSellerName("Alex Chen");
                item3.setSellerEmail("alex.chen@campus.edu");
                item3.setPhotoUrls(List.of(
                        "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?auto=format&fit=crop&w=800&q=80"
                ));
                repository.save(item3);

                // Listing 4: Bikes & Transport
                Listing item4 = new Listing();
                item4.setTitle("Trek FX 2 Disc Hybrid Campus Commuter Bike");
                item4.setCategory("Bikes & Transport");
                item4.setDescription("Lightweight aluminum frame, 18-speed Shimano drivetrain, hydraulic disc brakes. Includes U-lock and front/rear LED lights.");
                item4.setCondition(ItemCondition.LIKE_NEW);
                item4.setStartingPrice(new BigDecimal("280.00"));
                item4.setMinBidIncrement(new BigDecimal("20.00"));
                item4.setCurrentPrice(new BigDecimal("320.00"));
                item4.setClosingTime(LocalDateTime.now().plusDays(5).plusHours(18));
                item4.setStatus(ListingStatus.ACTIVE);
                item4.setSellerId("student-103");
                item4.setSellerName("Rohan Sharma");
                item4.setSellerEmail("rohan.sharma@campus.edu");
                item4.setPhotoUrls(List.of(
                        "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?auto=format&fit=crop&w=800&q=80"
                ));
                repository.save(item4);

                // Listing 5: Electronics (TI-84)
                Listing item5 = new Listing();
                item5.setTitle("Texas Instruments TI-84 Plus CE Graphing Calculator");
                item5.setCategory("Electronics");
                item5.setDescription("Color screen graphing calculator permitted on SAT, ACT, and AP exams. Mint condition with slide cover and USB charging cable.");
                item5.setCondition(ItemCondition.NEW);
                item5.setStartingPrice(new BigDecimal("85.00"));
                item5.setMinBidIncrement(new BigDecimal("5.00"));
                item5.setCurrentPrice(new BigDecimal("95.00"));
                item5.setClosingTime(LocalDateTime.now().plusDays(1).plusHours(6));
                item5.setStatus(ListingStatus.ACTIVE);
                item5.setSellerId("student-102");
                item5.setSellerName("Maya Patel");
                item5.setSellerEmail("maya.patel@campus.edu");
                item5.setPhotoUrls(List.of(
                        "https://images.unsplash.com/photo-1594980596870-8aa52a78d8cd?auto=format&fit=crop&w=800&q=80"
                ));
                repository.save(item5);

                System.out.println(">>> CampusBid Sample Listings Initialized in MySQL! <<<");
            }
        };
    }
}
