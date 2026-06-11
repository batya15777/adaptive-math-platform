package com.adaptive.server;

import com.adaptive.server.entity.User;
import com.adaptive.server.repository.UserRepository;
import com.adaptive.server.utils.GenerateHash;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Bean
    CommandLineRunner createMockUser(UserRepository userRepository) {
        return args -> {
            String testEmail = "test@example.com";

            // נבדוק אם המשתמש כבר קיים כדי לא ליצור כפילויות בכל הרצה
            if (!userRepository.existsByEmail(testEmail)) {

                String myUsername = "jon";
                String myPassword = "wow";

                // מייצרים את ה-Hash בדיוק כמו שהמערכת שלך מצפה
                String hash = GenerateHash.hashMd5(myUsername, myPassword);

                // יוצרים את המשתמש
                User mockUser = new User(
                        myUsername,
                        hash,
                        testEmail,
                        10,
                        "female",
                        LocalDateTime.now()
                );

                // שומרים ב-DB
                userRepository.save(mockUser);
                System.out.println("Mock user created successfully! You can now login with: " + testEmail + " / " + myPassword);
            }
        };
    }
}

