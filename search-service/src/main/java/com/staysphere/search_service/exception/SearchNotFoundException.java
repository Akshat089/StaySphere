package com.staysphere.search_service.exception;

public class SearchNotFoundException extends RuntimeException{
    public SearchNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
}
