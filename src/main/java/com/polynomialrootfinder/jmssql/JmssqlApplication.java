package com.polynomialrootfinder.jmssql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class JmssqlApplication {

	public static void main(String[] args) {
		SpringApplication.run(JmssqlApplication.class, args);
	}

	@GetMapping(value = "/")
	public String hello(){
		return "Hello from Spring";
	}

}
