package com.geros.backend.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByCodigo(String codigo);

    Page<Project> findByContractId(Long contractId, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.contract.id = :contractId AND " +
           "(LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Project> searchByContractIdAndKeyword(@Param("contractId") Long contractId, @Param("search") String search, Pageable pageable);
}
