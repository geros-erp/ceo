package com.geros.backend.menu;

import java.util.List;
import java.util.stream.Collectors;

public class MenuDTO {

    public static class Request {
        private String label;
        private String path;
        private String icon;
        private int sortOrder;
        private boolean active;
        private Long parentId;

        public String getLabel()    { return label; }
        public String getPath()     { return path; }
        public String getIcon()     { return icon; }
        public int getSortOrder()   { return sortOrder; }
        public boolean isActive()   { return active; }
        public Long getParentId()   { return parentId; }
    }

    public static class Response {
        private Long id;
        private String label;
        private String path;
        private String icon;
        private int sortOrder;
        private boolean active;
        private Long parentId;
        private List<Response> children;
        private List<String> roles;

        public Long getId()              { return id; }
        public String getLabel()         { return label; }
        public String getPath()          { return path; }
        public String getIcon()          { return icon; }
        public int getSortOrder()        { return sortOrder; }
        public boolean isActive()        { return active; }
        public Long getParentId()        { return parentId; }
        public List<Response> getChildren() { return children; }
        public List<String> getRoles()   { return roles; }

        public static Response from(MenuItem m, List<String> roles) {
            Response r = new Response();
            r.id        = m.getId();
            r.label     = m.getLabel();
            r.path      = m.getPath();
            r.icon      = m.getIcon();
            r.sortOrder = m.getSortOrder();
            r.active    = m.isActive();
            r.parentId  = m.getParent() != null ? m.getParent().getId() : null;
            r.roles     = roles;
            r.children  = m.getChildren().stream()
                           .map(c -> Response.from(c, List.of()))
                           .collect(Collectors.toList());
            return r;
        }
    }

    public static class PermissionRequest {
        private Long menuItemId;
        private Long roleId;
        private Boolean canView;
        private Boolean canCreate;
        private Boolean canUpdate;
        private Boolean canDelete;

        public Long getMenuItemId() { return menuItemId; }
        public Long getRoleId()     { return roleId; }
        public Boolean getCanView()   { return canView; }
        public Boolean getCanCreate() { return canCreate; }
        public Boolean getCanUpdate() { return canUpdate; }
        public Boolean getCanDelete() { return canDelete; }
    }

    public static class PermissionResponse {
        private Long id;
        private Long menuItemId;
        private String menuItemLabel;
        private String menuItemPath;
        private Long roleId;
        private String roleName;
        private boolean canView;
        private boolean canCreate;
        private boolean canUpdate;
        private boolean canDelete;

        public Long getId()              { return id; }
        public Long getMenuItemId()      { return menuItemId; }
        public String getMenuItemLabel() { return menuItemLabel; }
        public String getMenuItemPath()  { return menuItemPath; }
        public Long getRoleId()          { return roleId; }
        public String getRoleName()      { return roleName; }
        public boolean isCanView()       { return canView; }
        public boolean isCanCreate()     { return canCreate; }
        public boolean isCanUpdate()     { return canUpdate; }
        public boolean isCanDelete()     { return canDelete; }

        public static PermissionResponse from(MenuRolePermission p) {
            PermissionResponse r = new PermissionResponse();
            r.id             = p.getId();
            r.menuItemId     = p.getMenuItem().getId();
            r.menuItemLabel  = p.getMenuItem().getLabel();
            r.menuItemPath   = p.getMenuItem().getPath();
            r.roleId         = p.getRole().getId();
            r.roleName       = p.getRole().getName();
            r.canView        = p.isCanView();
            r.canCreate      = p.isCanCreate();
            r.canUpdate      = p.isCanUpdate();
            r.canDelete      = p.isCanDelete();
            return r;
        }
    }
}
