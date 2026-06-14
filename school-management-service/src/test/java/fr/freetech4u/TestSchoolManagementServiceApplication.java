package fr.freetech4u;

import org.springframework.boot.SpringApplication;

public class TestSchoolManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(SchoolManagementServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
