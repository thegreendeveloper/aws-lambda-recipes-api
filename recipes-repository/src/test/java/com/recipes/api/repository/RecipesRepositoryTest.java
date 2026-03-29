package com.recipes.api.repository;

import com.recipes.api.model.RecipeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RecipesRepositoryTest {

    @InjectMocks
    private RecipesRepository repository = new RecipesRepository();

    @Test
    void findAllReturnsSeedData() {
        List<RecipeEntity> all = repository.findAll();
        assertThat(all).hasSize(3);
    }

    @Test
    void findByIdHitReturnsEntity() {
        Optional<RecipeEntity> result = repository.findById("1");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Spaghetti Carbonara");
    }

    @Test
    void findByIdMissReturnsEmpty() {
        Optional<RecipeEntity> result = repository.findById("999");
        assertThat(result).isEmpty();
    }

    @Test
    void saveGeneratesIdAndReturnsEntity() {
        RecipeEntity entity = new RecipeEntity();
        entity.setName("Test Recipe");
        entity.setCuisine("Test");
        entity.setPrepTimeMinutes(10);
        entity.setIngredients(List.of("ingredient"));
        entity.setSteps(List.of("step"));

        RecipeEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Recipe");
    }

    @Test
    void saveAppendsToStore() {
        RecipeEntity entity = new RecipeEntity();
        entity.setName("New Recipe");
        entity.setCuisine("Fusion");
        entity.setPrepTimeMinutes(15);
        entity.setIngredients(List.of("a"));
        entity.setSteps(List.of("b"));

        repository.save(entity);

        assertThat(repository.findAll()).hasSize(4);
    }
}
