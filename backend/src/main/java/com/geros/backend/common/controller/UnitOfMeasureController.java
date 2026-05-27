package com.geros.backend.common.controller;

import com.geros.backend.common.service.UnitOfMeasureService;
import com.geros.backend.common.dto.UnitOfMeasureDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/common/units")
@RequiredArgsConstructor
public class UnitOfMeasureController {

    private final UnitOfMeasureService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UnitOfMeasureDTO> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UnitOfMeasureDTO get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UnitOfMeasureDTO create(@Valid @RequestBody UnitOfMeasureDTO request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UnitOfMeasureDTO update(@PathVariable Long id, @Valid @RequestBody UnitOfMeasureDTO request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
