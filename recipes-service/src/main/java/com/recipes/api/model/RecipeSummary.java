package com.recipes.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSummary {
    private String id;
    private String name;
    private String cuisine;
    private int prepTimeMinutes;
}
