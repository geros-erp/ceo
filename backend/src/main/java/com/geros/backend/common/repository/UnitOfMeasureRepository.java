package com.geros.backend.common.repository;

import com.geros.backend.common.model.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    boolean existsByDescription(String description);
    boolean existsByAbbreviation(String abbreviation);
}
