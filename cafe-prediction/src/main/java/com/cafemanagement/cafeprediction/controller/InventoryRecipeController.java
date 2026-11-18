package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.model.RecipeLine.Owner;
import com.cafemanagement.cafeprediction.service.InventoryRecipes;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory-recipes")
@PreAuthorize("hasRole('MANAGER')")
public class InventoryRecipeController {
    private final InventoryRecipes recipes;

    public InventoryRecipeController(InventoryRecipes recipes) {
        this.recipes = recipes;
    }

    @GetMapping("/{type}/{id}")
    public List<InventoryRecipes.Ingredient> get(@PathVariable Owner type, @PathVariable long id) {
        return recipes.get(type, id);
    }

    @PutMapping("/{type}/{id}")
    public List<InventoryRecipes.Ingredient> replace(@PathVariable Owner type, @PathVariable long id,
                                                     @Valid @RequestBody InventoryRecipes.Input input) {
        return recipes.replace(type, id, input);
    }
}
