package fr.fullstack.shopapp.controller;

import fr.fullstack.shopapp.model.Shop;
import fr.fullstack.shopapp.service.ShopService;
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
@RequestMapping("/api/v1/shops")
@Tag(name = "Shop Management", description = "APIs for managing shops")
public class ShopController {
    // TODO ADD PLAIN TEXT SEARCH FOR SHOP
    @Autowired
    private ShopService service;

    @Operation(summary = "Create a shop", description = "Create a new shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop created successfully",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Shop.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<Shop> createShop(@Valid @RequestBody Shop shop, Errors errors) throws Exception {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorValidation.getErrorValidationMessage(errors));
        }

        return ResponseEntity.ok(service.createShop(shop));
    }

    @Operation(summary = "Delete a shop by id", description = "Delete a shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Shop deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied")
    })
    @DeleteMapping("/{id}")
    public HttpStatus deleteShop(@PathVariable long id) throws Exception {
        service.deleteShopById(id);
        return HttpStatus.NO_CONTENT;
    }

    @Operation(summary = "Get all shops", description = "Get shops with pagination with optional filtering and sorting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shops retrieved successfully",
                    content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Page.class))})
    })
    @GetMapping
    public ResponseEntity<Page<Shop>> getAllShops(
            @ParameterObject Pageable pageable,
            @Parameter(description = "Plain text search on name of the shop") @RequestParam Optional<String> search,
            @Parameter(description = "Sort shops by field") @RequestParam Optional<String> sortBy,
            @Parameter(description = "Filter shops based on vacation status") @RequestParam Optional<Boolean> inVacations,
            @Parameter(description = "Filter shops created after a date (YYYY-MM-DD)") @RequestParam Optional<String> createdAfter,
            @Parameter(description = "Filter shops created before a date (YYYY-MM-DD)") @RequestParam Optional<String> createdBefore) {

        return ResponseEntity.ok(
                service.getShopList(search, sortBy, inVacations, createdAfter, createdBefore, pageable)
        );
    }

    @Operation(summary = "Get a shop by id", description = "Get a shop by id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop found",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = Shop.class))}),
            @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Shop> getShopById(@PathVariable long id) throws Exception {
        return ResponseEntity.ok().body(service.getShopById(id));
    }

    @Operation(summary = "Update a shop", description = "Update a shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shop updated successfully",
                    content = {@Content(mediaType = "application/json",
                    schema = @Schema(implementation = Shop.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping
    public ResponseEntity<Shop> updateShop(@Valid @RequestBody Shop shop, Errors errors) throws Exception {
        if (errors.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ErrorValidation.getErrorValidationMessage(errors));
        }

        return ResponseEntity.ok().body(service.updateShop(shop));
    }
}
