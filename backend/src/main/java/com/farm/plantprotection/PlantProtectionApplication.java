package com.farm.plantprotection;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.farm.plantprotection.mapper")
public class PlantProtectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlantProtectionApplication.class, args);
    }
}
