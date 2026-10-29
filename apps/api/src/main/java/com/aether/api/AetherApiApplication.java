package com.aether.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Financial services use JdbcTemplate and JDBC transactions; no JPA repositories are consumed.
@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
})
public class AetherApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AetherApiApplication.class, args);
    }
}
