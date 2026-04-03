package com.geros.backend.role;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class RoleDTO {

    public static class Request {
        @NotBlank private String name;
        private String description;
        private boolean privileged;
        private String profileType;
        private List<PermissionRequest> permissions;

        public String getName()        { return name; }
        public String getDescription() { return description; }
        public boolean isPrivileged()  { return privileged; }
        public String getProfileType() { return profileType; }
        public List<PermissionRequest> getPermissions() { return permissions; }
    }

    public static class Response {
        private Long id;
        private String name;
        private String description;
        private boolean privileged;
        private String profileType;
        private List<PermissionResponse> permissions;

        public Long getId()            { return id; }
        public String getName()        { return name; }
        public String getDescription() { return description; }
        public boolean isPrivileged()  { return privileged; }
        public String getProfileType() { return profileType; }
        public List<PermissionResponse> getPermissions() { return permissions; }

        public static Response from(Role r, List<PermissionResponse> permissions) {
            Response res = new Response();
            res.id          = r.getId();
            res.name        = r.getName();
            res.description = r.getDescription();
            res.privileged  = r.isPrivileged();
            res.profileType = r.getProfileType().name();
            res.permissions = permissions;
            return res;
        }
    }

    public static class PermissionRequest {
        private Long menuItemId;
        private boolean canView;
        private boolean canCreate;
        private boolean canUpdate;
        private boolean canDelete;

        public Long getMenuItemId()      { return menuItemId; }
        public boolean isCanView()       { return canView; }
        public boolean isCanCreate()     { return canCreate; }
        public boolean isCanUpdate()     { return canUpdate; }
        public boolean isCanDelete()     { return canDelete; }
    }

    public static class PermissionResponse {
        private Long menuItemId;
        private String menuItemLabel;
        private String menuItemPath;
        private Long parentId;
        private boolean active;
        private boolean canView;
        private boolean canCreate;
        private boolean canUpdate;
        private boolean canDelete;

        public Long getMenuItemId()      { return menuItemId; }
        public String getMenuItemLabel() { return menuItemLabel; }
        public String getMenuItemPath()  { return menuItemPath; }
        public Long getParentId()        { return parentId; }
        public boolean isActive()        { return active; }
        public boolean isCanView()       { return canView; }
        public boolean isCanCreate()     { return canCreate; }
        public boolean isCanUpdate()     { return canUpdate; }
        public boolean isCanDelete()     { return canDelete; }

        public static PermissionResponse from(com.geros.backend.menu.MenuRolePermission permission) {
            PermissionResponse response = new PermissionResponse();
            response.menuItemId = permission.getMenuItem().getId();
            response.menuItemLabel = permission.getMenuItem().getLabel();
            response.menuItemPath = permission.getMenuItem().getPath();
            response.parentId = permission.getMenuItem().getParent() != null ? permission.getMenuItem().getParent().getId() : null;
            response.active = permission.getMenuItem().isActive();
            response.canView = permission.isCanView();
            response.canCreate = permission.isCanCreate();
            response.canUpdate = permission.isCanUpdate();
            response.canDelete = permission.isCanDelete();
            return response;
        }
    }

    public static class PrivilegeCatalogItem {
        private Long menuItemId;
        private String label;
        private String path;
        private Long parentId;
        private boolean active;
        private int sortOrder;

        public Long getMenuItemId() { return menuItemId; }
        public String getLabel()    { return label; }
        public String getPath()     { return path; }
        public Long getParentId()   { return parentId; }
        public boolean isActive()   { return active; }
        public int getSortOrder()   { return sortOrder; }

        public static PrivilegeCatalogItem from(com.geros.backend.menu.MenuItem item) {
            PrivilegeCatalogItem catalogItem = new PrivilegeCatalogItem();
            catalogItem.menuItemId = item.getId();
            catalogItem.label = item.getLabel();
            catalogItem.path = item.getPath();
            catalogItem.parentId = item.getParent() != null ? item.getParent().getId() : null;
            catalogItem.active = item.isActive();
            catalogItem.sortOrder = item.getSortOrder();
            return catalogItem;
        }
    }
}
