package fr.fullstack.shopapp.controller;

import fr.fullstack.shopapp.model.Product;
import fr.fullstack.shopapp.service.ProductService;
import fr.fullstack.shopapp.util.ErrorValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    @Autowired
    private ProductService service;

    @Operation(summary = "Create a product", description = "Create a new product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Product.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product, Errors errors) throws Exception {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorValidation.getErrorValidationMessage(errors));
        }

        return ResponseEntity.ok(service.createProduct(product));
    }

    @Operation(summary = "Delete a product by id", description = "Delete a product")
    @DeleteMapping("/{id}")
    public HttpStatus deleteProduct(@PathVariable long id) throws Exception {
        service.deleteProductById(id);
        return HttpStatus.NO_CONTENT;
    }

    @Operation(summary = "Get a product by id", description = "Get a product by id")
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable long id) throws Exception {
        return ResponseEntity.ok().body(service.getProductById(id));
    }

    @Operation(
            summary = "Get products",
            description = "Get products with pagination, optionally filtered by shop/category"
    )
    @GetMapping
    public ResponseEntity<Page<Product>> getProductsOfShop(
            @ParameterObject Pageable pageable,
            @Parameter(description = "Id of the shop") @RequestParam Optional<Long> shopId,
            @Parameter(description = "Id of the category") @RequestParam Optional<Long> categoryId) {
        return ResponseEntity.ok(service.getShopProductList(shopId, categoryId, pageable));
    }

    @Operation(summary = "Update a product", description = "Update a product")
    @PutMapping
    public ResponseEntity<Product> updateProduct(@Valid @RequestBody Product product, Errors errors) throws Exception {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorValidation.getErrorValidationMessage(errors));
        }

        return ResponseEntity.ok().body(service.updateProduct(product));
    }
}
