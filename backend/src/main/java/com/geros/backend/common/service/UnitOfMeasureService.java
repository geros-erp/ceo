package com.geros.backend.common.service;

import com.geros.backend.common.model.UnitOfMeasure;
import com.geros.backend.common.repository.UnitOfMeasureRepository;
import com.geros.backend.common.dto.UnitOfMeasureDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitOfMeasureService {

    private final UnitOfMeasureRepository repository;

    @Transactional(readOnly = true)
    public List<UnitOfMeasureDTO> findAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnitOfMeasureDTO findById(Long id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Unidad de medida no encontrada: " + id));
    }

    @Transactional
    public UnitOfMeasureDTO create(UnitOfMeasureDTO request) {
        if (repository.existsByDescription(request.getDescription())) {
            throw new RuntimeException("Ya existe una unidad con la descripción: " + request.getDescription());
        }
        if (repository.existsByAbbreviation(request.getAbbreviation())) {
            throw new RuntimeException("Ya existe una unidad con la abreviatura: " + request.getAbbreviation());
        }

        UnitOfMeasure uom = new UnitOfMeasure();
        updateEntityFromDto(uom, request);
        uom.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        uom.setAllowsDecimal(request.getAllowsDecimal() != null ? request.getAllowsDecimal() : false);
        uom.setCreatedBy(getCurrentUser());
        uom.setUpdatedBy(getCurrentUser());
        return mapToResponse(repository.save(uom));
    }

    @Transactional
    public UnitOfMeasureDTO update(Long id, UnitOfMeasureDTO request) {
        UnitOfMeasure uom = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unidad no encontrada"));

        // Validar unicidad si cambian los valores
        if (!uom.getDescription().equals(request.getDescription()) && repository.existsByDescription(request.getDescription())) {
            throw new RuntimeException("La nueva descripción ya está en uso");
        }
        if (!uom.getAbbreviation().equals(request.getAbbreviation()) && repository.existsByAbbreviation(request.getAbbreviation())) {
            throw new RuntimeException("La nueva abreviatura ya está en uso");
        }

        updateEntityFromDto(uom, request);
        uom.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        uom.setAllowsDecimal(request.getAllowsDecimal() != null ? request.getAllowsDecimal() : false);
        uom.setUpdatedBy(getCurrentUser());

        return mapToResponse(repository.save(uom));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new RuntimeException("La unidad no existe");
        repository.deleteById(id);
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "system";
    }

    private void updateEntityFromDto(UnitOfMeasure entity, UnitOfMeasureDTO dto) {
        entity.setDescription(dto.getDescription());
        entity.setAbbreviation(dto.getAbbreviation());
    }

    private UnitOfMeasureDTO mapToResponse(UnitOfMeasure uom) {
        return UnitOfMeasureDTO.builder()
                .id(uom.getId())
                .description(uom.getDescription())
                .abbreviation(uom.getAbbreviation())
                .isActive(uom.isActive())
                .allowsDecimal(uom.isAllowsDecimal())
                .build();
    }
}
