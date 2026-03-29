package com.recipes.api.repository;

import com.recipes.api.model.RecipeEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class RecipesRepository {

    private final List<RecipeEntity> store = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    public RecipesRepository() {
        store.add(new RecipeEntity(
                String.valueOf(idCounter.incrementAndGet()),
                "Spaghetti Carbonara",
                "Italian",
                30,
                List.of("spaghetti", "eggs", "pancetta", "parmesan", "black pepper"),
                List.of("Cook pasta", "Fry pancetta", "Mix eggs and cheese", "Combine all")
        ));
        store.add(new RecipeEntity(
                String.valueOf(idCounter.incrementAndGet()),
                "Chicken Tikka Masala",
                "Indian",
                45,
                List.of("chicken", "tomato sauce", "cream", "spices"),
                List.of("Marinate chicken", "Grill chicken", "Make sauce", "Combine and simmer")
        ));
        store.add(new RecipeEntity(
                String.valueOf(idCounter.incrementAndGet()),
                "Beef Tacos",
                "Mexican",
                20,
                List.of("ground beef", "taco shells", "cheese", "lettuce", "salsa"),
                List.of("Brown beef", "Season beef", "Assemble tacos")
        ));
    }

    public List<RecipeEntity> findAll() {
        return new ArrayList<>(store);
    }

    public Optional<RecipeEntity> findById(String id) {
        return store.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public RecipeEntity save(RecipeEntity entity) {
        entity.setId(String.valueOf(idCounter.incrementAndGet()));
        store.add(entity);
        return entity;
    }
}
