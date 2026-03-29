package com.recipes.api.service;

import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.model.RecipeEntity;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.repository.RecipesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipesServiceTest {

    @Mock
    private RecipesRepository repository;

    @InjectMocks
    private RecipesService service;

    @Test
    void listRecipesReturnsSummaryList() {
        when(repository.findAll()).thenReturn(List.of(
                RecipeEntity.builder()
                        .id("1").name("Pasta").cuisine("Italian")
                        .prepTimeMinutes(30).ingredients(List.of("pasta")).steps(List.of("cook"))
                        .build()
        ));

        List<RecipeSummary> result = service.listRecipes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1");
        assertThat(result.get(0).getName()).isEqualTo("Pasta");
        assertThat(result.get(0).getCuisine()).isEqualTo("Italian");
        assertThat(result.get(0).getPrepTimeMinutes()).isEqualTo(30);
    }

    @Test
    void getRecipeByIdReturnsDetail() {
        when(repository.findById("1")).thenReturn(Optional.of(
                RecipeEntity.builder()
                        .id("1").name("Pasta").cuisine("Italian")
                        .prepTimeMinutes(30).ingredients(List.of("pasta")).steps(List.of("cook"))
                        .build()
        ));

        RecipeDetail result = service.getRecipeById("1");

        assertThat(result.getId()).isEqualTo("1");
        assertThat(result.getName()).isEqualTo("Pasta");
        assertThat(result.getIngredients()).containsExactly("pasta");
        assertThat(result.getSteps()).containsExactly("cook");
    }

    @Test
    void getRecipeByIdNotFoundThrowsException() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRecipeById("999"))
                .isInstanceOf(RecipeNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void createRecipeCallsSaveReturnsDetail() {
        RecipeEntity saved = RecipeEntity.builder()
                .id("4").name("Ramen").cuisine("Japanese")
                .prepTimeMinutes(60).ingredients(List.of("noodles")).steps(List.of("boil"))
                .build();
        when(repository.save(any(RecipeEntity.class))).thenReturn(saved);

        RecipeDetail result = service.createRecipe("Ramen", "Japanese", 60,
                List.of("noodles"), List.of("boil"));

        verify(repository).save(any(RecipeEntity.class));
        assertThat(result.getId()).isEqualTo("4");
        assertThat(result.getName()).isEqualTo("Ramen");
    }
}
