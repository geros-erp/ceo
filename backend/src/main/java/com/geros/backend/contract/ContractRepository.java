package com.geros.backend.contract;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByCodigo(String codigo);

    @Query("SELECT c FROM Contract c WHERE " +
           "LOWER(c.codigo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.contratista) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Contract> searchContracts(@Param("search") String search, Pageable pageable);
}
