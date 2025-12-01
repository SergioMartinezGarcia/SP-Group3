package com.unipd.dei.sp.Group3;

import org.springframework.boot.SpringApplication;

public class TestGroup3Application {

	public static void main(String[] args) {
		SpringApplication.from(Group3Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
