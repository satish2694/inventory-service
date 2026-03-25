package com.inventory.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "serviceB-client", url = "http://localhost:8082")
public interface ServiceBFeignClient {
    
    @GetMapping("/serviceB/displayMessage")
    String displayMessage(@CookieValue(value = "chocolate") String chocolateCookie);
}
