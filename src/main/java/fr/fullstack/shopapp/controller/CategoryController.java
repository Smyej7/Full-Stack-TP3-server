package fr.fullstack.shopapp.controller;

import fr.fullstack.shopapp.model.Category;
import fr.fullstack.shopapp.service.CategoryService;
import fr.fullstack.shopapp.util.ErrorValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "APIs for managing categories")
public class CategoryController {

    @Autowired
    private CategoryService service;

    @Operation(summary = "Create a category", description = "Create a new category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Category.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody Category category, Errors errors) throws Exception {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorValidation.getErrorValidationMessage(errors));
        }

        return ResponseEntity.ok(service.createCategory(category));
    }

    @Operation(summary = "Delete a category by id", description = "Delete a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid id")
    })
    @DeleteMapping("/{id}")
    public HttpStatus deleteCategory(@PathVariable long id) throws Exception {
            service.deleteCategoryById(id);
            return HttpStatus.NO_CONTENT;
    }

    @Operation(summary = "Get categories", description = "Get categories with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Page.class))})
    })
    @GetMapping
    public ResponseEntity<Page<Category>> getAllCategories(
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.getCategoryList(pageable));
    }

    @Operation(summary = "Get a category by id", description = "Retrieve a category by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Category.class))}),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable long id) throws Exception {
        return ResponseEntity.ok().body(service.getCategoryById(id));
    }

    @Operation(summary = "Update a category", description = "Update a category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Category.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping
    public ResponseEntity<Category> updateCategory(@Valid @RequestBody Category category, Errors errors) throws Exception {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorValidation.getErrorValidationMessage(errors));
        }

        return ResponseEntity.ok().body(service.updateCategory(category));
    }
}
