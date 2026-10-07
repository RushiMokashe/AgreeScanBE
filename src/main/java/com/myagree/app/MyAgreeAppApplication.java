package com.myagree.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyAgreeAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyAgreeAppApplication.class, args);
        System.out.println("Project In running state ");
    }
}
