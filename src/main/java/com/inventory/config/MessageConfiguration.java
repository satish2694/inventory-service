package com.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@ConfigurationProperties(prefix = "message")
@RefreshScope
public class MessageConfiguration {
    private String displayMessage;

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MessageConfiguration that = (MessageConfiguration) o;
        return Objects.equals(displayMessage, that.displayMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(displayMessage);
    }
}
