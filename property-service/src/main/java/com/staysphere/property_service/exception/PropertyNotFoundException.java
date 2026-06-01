package com.staysphere.property_service.exception;

public class PropertyNotFoundException extends RuntimeException{
    public PropertyNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
