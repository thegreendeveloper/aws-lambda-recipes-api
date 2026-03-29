package com.recipes.api.exception;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(String id) {
        super("Recipe not found: " + id);
    }
}
