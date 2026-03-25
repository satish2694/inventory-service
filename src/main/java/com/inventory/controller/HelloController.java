package com.inventory.controller;

import com.inventory.config.MessageConfiguration;
import com.inventory.service.ServiceBCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory-service")
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @Autowired
    private MessageConfiguration messageConfiguration;

    @Autowired
    private ServiceBCallerService serviceBCallerService;

    @GetMapping("/displayMessage")
    public ResponseEntity<String> showMessage() {
        logger.info("Inventory service displayMessage endpoint called");
        return ResponseEntity.ok("Inventory Service controller executed " + messageConfiguration.getDisplayMessage());
    }

    @GetMapping("/callServiceB")
    public ResponseEntity<String> callServiceB(
            @RequestParam(value = "cookie", defaultValue = "dark") String cookieValue) {
        logger.info("Calling ServiceB with cookie: {}", cookieValue);
        String response = serviceBCallerService.callServiceB(cookieValue);
        return ResponseEntity.ok("Response from ServiceB: " + response);
    }

    @GetMapping("/callServiceBWithFallback")
    public ResponseEntity<String> callServiceBWithFallback(
            @RequestParam(value = "cookie", defaultValue = "dark") String cookieValue) {
        logger.info("Calling ServiceB with fallback strategy, cookie: {}", cookieValue);
        String response = serviceBCallerService.callServiceBWithFallback(cookieValue);
        return ResponseEntity.ok(response);
    }
}
