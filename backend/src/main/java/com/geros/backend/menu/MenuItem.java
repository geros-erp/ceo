package com.geros.backend.menu;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_items", schema = "menu")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 200)
    private String path;

    @Column(length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MenuItem parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<MenuItem> children = new ArrayList<>();

    public Long getId()                    { return id; }

    public String getLabel()               { return label; }
    public void setLabel(String label)     { this.label = label; }

    public String getPath()                { return path; }
    public void setPath(String path)       { this.path = path; }

    public String getIcon()                { return icon; }
    public void setIcon(String icon)       { this.icon = icon; }

    public int getSortOrder()              { return sortOrder; }
    public void setSortOrder(int sortOrder){ this.sortOrder = sortOrder; }

    public boolean isActive()              { return active; }
    public void setActive(boolean active)  { this.active = active; }

    public MenuItem getParent()            { return parent; }
    public void setParent(MenuItem parent) { this.parent = parent; }

    public List<MenuItem> getChildren()    { return children; }
}
