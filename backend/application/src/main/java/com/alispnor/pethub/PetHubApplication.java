package com.alispnor.pethub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.alispnor.pethub")
@EnableJpaRepositories(basePackages = "com.alispnor.pethub")
@EntityScan(basePackages = "com.alispnor.pethub")
@EnableScheduling
public class PetHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetHubApplication.class, args);
    }
}
