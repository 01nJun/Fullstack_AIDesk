package com.desk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@SpringBootApplication
@EnableJpaRepositories(
        basePackages = "com.desk.repository.mariadb"  // MariaDB Repository만
)
public class DeskApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeskApplication.class, args);
	}

}
