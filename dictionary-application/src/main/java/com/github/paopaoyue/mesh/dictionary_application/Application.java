package com.github.paopaoyue.mesh.dictionary_application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication()
public class Application {

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(Application.class, args);
		for (String name : context.getBeanDefinitionNames()) {
			System.out.println(name);
		}
	}

}
