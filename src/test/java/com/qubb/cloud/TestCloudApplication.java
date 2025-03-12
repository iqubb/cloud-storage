package com.qubb.cloud;

import org.springframework.boot.SpringApplication;

public class TestCloudApplication {
    public static void main(String[] args) {
        SpringApplication.from(CloudApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
