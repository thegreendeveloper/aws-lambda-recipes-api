package com.recipes.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeDetail {
    private String id;
    private String name;
    private String cuisine;
    private int prepTimeMinutes;
    private List<String> ingredients;
    private List<String> steps;
}
