package com.salang.matching_poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MatchingPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchingPocApplication.class, args);
	}

}
