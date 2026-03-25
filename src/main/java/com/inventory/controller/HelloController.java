package com.inventory.controller;

import com.inventory.common.dto.ApiResponse;
import com.inventory.config.MessageConfiguration;
import com.inventory.service.ServiceBCallerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for inventory service endpoints.
 * Follows Single Responsibility Principle - only handles HTTP requests.
 */
@RestController
@RequestMapping("/inventory-service")
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    private final MessageConfiguration messageConfiguration;
    private final ServiceBCallerService serviceBCallerService;

    public HelloController(MessageConfiguration messageConfiguration, 
                         ServiceBCallerService serviceBCallerService) {
        this.messageConfiguration = messageConfiguration;
        this.serviceBCallerService = serviceBCallerService;
    }

    @GetMapping("/displayMessage")
    public ResponseEntity<ApiResponse<String>> showMessage() {
        logger.info("Inventory service displayMessage endpoint called");
        String message = "Inventory Service controller executed " + messageConfiguration.getDisplayMessage();
        return ResponseEntity.ok(ApiResponse.success(message, "Message retrieved"));
    }

    @GetMapping("/callServiceB")
    public ResponseEntity<ApiResponse<String>> callServiceB(
            @RequestParam(value = "cookie", defaultValue = "dark") String cookieValue) {
        logger.info("Calling ServiceB with cookie: {}", cookieValue);
        String response = serviceBCallerService.callServiceB(cookieValue);
        return ResponseEntity.ok(ApiResponse.success(response, "ServiceB call successful"));
    }

    @GetMapping("/callServiceBWithFallback")
    public ResponseEntity<ApiResponse<String>> callServiceBWithFallback(
            @RequestParam(value = "cookie", defaultValue = "dark") String cookieValue) {
        logger.info("Calling ServiceB with fallback strategy, cookie: {}", cookieValue);
        String response = serviceBCallerService.callServiceBWithFallback(cookieValue);
        return ResponseEntity.ok(ApiResponse.success(response, "ServiceB call completed"));
    }
}
