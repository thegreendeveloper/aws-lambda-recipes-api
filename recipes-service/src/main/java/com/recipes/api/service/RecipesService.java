package com.recipes.api.service;

import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.model.RecipeEntity;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.repository.RecipesRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipesService {

    private final RecipesRepository repository;

    public RecipesService(RecipesRepository repository) {
        this.repository = repository;
    }

    public List<RecipeSummary> listRecipes() {
        return repository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    public RecipeDetail getRecipeById(String id) {
        return repository.findById(id)
                .map(this::toDetail)
                .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    public RecipeDetail createRecipe(String name, String cuisine, int prepTimeMinutes,
                                     List<String> ingredients, List<String> steps) {
        RecipeEntity entity = RecipeEntity.builder()
                .name(name)
                .cuisine(cuisine)
                .prepTimeMinutes(prepTimeMinutes)
                .ingredients(ingredients)
                .steps(steps)
                .build();
        return toDetail(repository.save(entity));
    }

    private RecipeSummary toSummary(RecipeEntity entity) {
        return RecipeSummary.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cuisine(entity.getCuisine())
                .prepTimeMinutes(entity.getPrepTimeMinutes())
                .build();
    }

    private RecipeDetail toDetail(RecipeEntity entity) {
        return RecipeDetail.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cuisine(entity.getCuisine())
                .prepTimeMinutes(entity.getPrepTimeMinutes())
                .ingredients(entity.getIngredients())
                .steps(entity.getSteps())
                .build();
    }
}
