package com.recipes.api.handler;

import com.recipes.api.generated.api.RecipesApi;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.generated.model.RecipeDetailResponse;
import com.recipes.api.generated.model.RecipeSummaryResponse;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.service.RecipesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecipesController implements RecipesApi {

    private final RecipesService service;

    public RecipesController(RecipesService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<RecipeSummaryResponse>> listRecipes() {
        List<RecipeSummaryResponse> responses = service.listRecipes().stream()
                .map(this::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<RecipeDetailResponse> getRecipeById(String id) {
        return ResponseEntity.ok(toDetailResponse(service.getRecipeById(id)));
    }

    @Override
    public ResponseEntity<RecipeDetailResponse> createRecipe(CreateRecipeRequest request) {
        RecipeDetail detail = service.createRecipe(
                request.getName(),
                request.getCuisine(),
                request.getPrepTimeMinutes(),
                request.getIngredients(),
                request.getSteps()
        );
        return ResponseEntity.status(201).body(toDetailResponse(detail));
    }

    private RecipeSummaryResponse toSummaryResponse(RecipeSummary summary) {
        return RecipeSummaryResponse.builder()
                .id(summary.getId())
                .name(summary.getName())
                .cuisine(summary.getCuisine())
                .prepTimeMinutes(summary.getPrepTimeMinutes())
                .build();
    }

    private RecipeDetailResponse toDetailResponse(RecipeDetail detail) {
        return RecipeDetailResponse.builder()
                .id(detail.getId())
                .name(detail.getName())
                .cuisine(detail.getCuisine())
                .prepTimeMinutes(detail.getPrepTimeMinutes())
                .ingredients(detail.getIngredients())
                .steps(detail.getSteps())
                .build();
    }
}
