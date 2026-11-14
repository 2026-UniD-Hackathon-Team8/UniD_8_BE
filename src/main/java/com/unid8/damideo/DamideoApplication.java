package com.unid8.damideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DamideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DamideoApplication.class, args);
    }
}
