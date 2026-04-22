package com.fitness.aiservice;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EntityScan("com.fitness.aiservice.model")
@EnableJpaRepositories("com.fitness.aiservice.repository")
@EnableRabbit
public class AiserviceApplication {

	public static void main(String[] args) {

		SpringApplication.run(AiserviceApplication.class, args);
	}

}
