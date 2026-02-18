package com.proyectospringboot.proyectosaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ProyectoSaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProyectoSaasApplication.class, args);
	}

}
