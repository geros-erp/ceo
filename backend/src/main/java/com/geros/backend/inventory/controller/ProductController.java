package com.geros.backend.inventory.controller;

import com.geros.backend.inventory.dto.ProductDTO;
import com.geros.backend.inventory.service.ProductService;
import com.geros.backend.common.service.UnitOfMeasureService;
import com.geros.backend.common.dto.UnitOfMeasureDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UnitOfMeasureService unitOfMeasureService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProductDTO.Response> list() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDTO.Response get(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDTO.Response create(@Valid @RequestBody ProductDTO.Request request) {
        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDTO.Response update(@PathVariable Long id, @Valid @RequestBody ProductDTO.Request request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        productService.deleteProduct(id);
    }

    @GetMapping("/units")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UnitOfMeasureDTO> getUnits() {
        return unitOfMeasureService.findAll();
    }
}
