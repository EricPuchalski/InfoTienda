package com.infotienda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class InfotiendaApplication {

	public static void main(String[] args) {
		SpringApplication.run(InfotiendaApplication.class, args);
	}

}
