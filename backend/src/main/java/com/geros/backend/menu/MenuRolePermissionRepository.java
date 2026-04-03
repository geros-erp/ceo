package com.geros.backend.menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MenuRolePermissionRepository extends JpaRepository<MenuRolePermission, Long> {

    @Query("SELECT p FROM MenuRolePermission p WHERE p.role.name IN :roleNames")
    List<MenuRolePermission> findByRoleNames(@Param("roleNames") Set<String> roleNames);

    @Query("SELECT p FROM MenuRolePermission p WHERE p.menuItem.id = :menuItemId")
    List<MenuRolePermission> findByMenuItemId(@Param("menuItemId") Long menuItemId);

    @Query("""
            SELECT p
            FROM MenuRolePermission p
            WHERE p.menuItem.active = true
              AND p.menuItem.path = :path
              AND p.role.name IN :roleNames
            """)
    List<MenuRolePermission> findActivePermissionsForPath(@Param("path") String path, @Param("roleNames") Set<String> roleNames);

    List<MenuRolePermission> findByRoleId(Long roleId);

    boolean existsByMenuItemIdAndRoleId(Long menuItemId, Long roleId);

    void deleteByMenuItemIdAndRoleId(Long menuItemId, Long roleId);

    void deleteByRoleId(Long roleId);
}
