package dev.careeros;

import org.springframework.boot.SpringApplication;

public class TestCareerosApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(CareerosApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
