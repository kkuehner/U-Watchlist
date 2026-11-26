package com.example.Investment.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(Long id) {
        super("No category with id " + id);
    }
}
