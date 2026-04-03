package com.geros.backend.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT m FROM MenuItem m WHERE m.parent IS NULL ORDER BY m.sortOrder ASC")
    List<MenuItem> findRootItems();

    @Query("SELECT m FROM MenuItem m WHERE m.parent IS NULL AND m.active = true ORDER BY m.sortOrder ASC")
    List<MenuItem> findActiveRootItems();
}
