package dev.jiwon.popup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhateverPopupApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhateverPopupApplication.class, args);
    }

}
