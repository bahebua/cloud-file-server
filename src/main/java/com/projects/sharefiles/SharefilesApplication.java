package com.projects.sharefiles;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SharefilesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SharefilesApplication.class, args);
	}

}
