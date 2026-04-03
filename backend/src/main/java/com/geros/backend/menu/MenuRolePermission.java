package com.geros.backend.menu;

import com.geros.backend.role.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "menu_role_permissions", schema = "menu",
       uniqueConstraints = @UniqueConstraint(columnNames = {"menu_item_id", "role_id"}))
public class MenuRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "can_view", nullable = false)
    private boolean canView = true;

    @Column(name = "can_create", nullable = false)
    private boolean canCreate = false;

    @Column(name = "can_update", nullable = false)
    private boolean canUpdate = false;

    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;

    public MenuRolePermission() {}

    public MenuRolePermission(MenuItem menuItem, Role role) {
        this.menuItem = menuItem;
        this.role     = role;
    }

    public MenuRolePermission(MenuItem menuItem, Role role,
                              boolean canView, boolean canCreate, boolean canUpdate, boolean canDelete) {
        this.menuItem = menuItem;
        this.role = role;
        this.canView = canView;
        this.canCreate = canCreate;
        this.canUpdate = canUpdate;
        this.canDelete = canDelete;
    }

    public Long getId()          { return id; }
    public MenuItem getMenuItem(){ return menuItem; }
    public Role getRole()        { return role; }
    public boolean isCanView()   { return canView; }
    public boolean isCanCreate() { return canCreate; }
    public boolean isCanUpdate() { return canUpdate; }
    public boolean isCanDelete() { return canDelete; }

    public void setCanView(boolean canView)       { this.canView = canView; }
    public void setCanCreate(boolean canCreate)   { this.canCreate = canCreate; }
    public void setCanUpdate(boolean canUpdate)   { this.canUpdate = canUpdate; }
    public void setCanDelete(boolean canDelete)   { this.canDelete = canDelete; }

    public boolean grants(String privilege) {
        return switch (privilege) {
            case "READ" -> canView;
            case "CREATE" -> canCreate;
            case "UPDATE" -> canUpdate;
            case "DELETE" -> canDelete;
            default -> false;
        };
    }
}
