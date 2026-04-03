package com.geros.backend.role;

import jakarta.persistence.*;

@Entity
@Table(name = "roles", schema = "auth")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private boolean privileged = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_type", nullable = false, length = 20)
    private RoleProfileType profileType = RoleProfileType.STANDARD;

    public Role() {}

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPrivileged() { return privileged; }
    public void setPrivileged(boolean privileged) { this.privileged = privileged; }

    public RoleProfileType getProfileType() { return profileType; }
    public void setProfileType(RoleProfileType profileType) { this.profileType = profileType; }
}
