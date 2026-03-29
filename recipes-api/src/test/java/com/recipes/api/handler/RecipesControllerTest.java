package com.recipes.api.handler;

import com.recipes.api.exception.RecipeNotFoundException;
import com.recipes.api.generated.model.CreateRecipeRequest;
import com.recipes.api.model.RecipeDetail;
import com.recipes.api.model.RecipeSummary;
import com.recipes.api.service.RecipesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipesControllerTest {

    @Mock
    private RecipesService service;

    @InjectMocks
    private RecipesController controller;

    @Test
    void listRecipesReturns200WithBody() {
        when(service.listRecipes()).thenReturn(List.of(
                RecipeSummary.builder().id("1").name("Pasta").cuisine("Italian").prepTimeMinutes(30).build()
        ));

        var response = controller.listRecipes();

        var body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull().hasSize(1);
        assertThat(body.get(0).getId()).isEqualTo("1");
    }

    @Test
    void getRecipeByIdReturns200WithBody() {
        when(service.getRecipeById("1")).thenReturn(
                RecipeDetail.builder()
                        .id("1").name("Pasta").cuisine("Italian").prepTimeMinutes(30)
                        .ingredients(List.of("pasta")).steps(List.of("cook"))
                        .build()
        );

        var response = controller.getRecipeById("1");

        var body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo("1");
    }

    @Test
    void getRecipeByIdNotFoundThrowsException() {
        when(service.getRecipeById("999")).thenThrow(new RecipeNotFoundException("999"));

        assertThatThrownBy(() -> controller.getRecipeById("999"))
                .isInstanceOf(RecipeNotFoundException.class);
    }

    @Test
    void createRecipeReturns201WithBody() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setName("Ramen");
        request.setCuisine("Japanese");
        request.setPrepTimeMinutes(60);
        request.setIngredients(List.of("noodles", "broth"));
        request.setSteps(List.of("Boil broth", "Cook noodles"));

        when(service.createRecipe("Ramen", "Japanese", 60,
                List.of("noodles", "broth"), List.of("Boil broth", "Cook noodles")))
                .thenReturn(RecipeDetail.builder()
                        .id("4").name("Ramen").cuisine("Japanese").prepTimeMinutes(60)
                        .ingredients(List.of("noodles", "broth"))
                        .steps(List.of("Boil broth", "Cook noodles"))
                        .build());

        var response = controller.createRecipe(request);

        var body = response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo("4");
        assertThat(body.getName()).isEqualTo("Ramen");
    }
}
