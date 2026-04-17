package com.streamflow.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class StreamflowProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamflowProcessorApplication.class, args);
    }
}
