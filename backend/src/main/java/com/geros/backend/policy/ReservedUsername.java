package com.geros.backend.policy;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "reserved_usernames", schema = "auth")
public class ReservedUsername {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String description;

    @Column(name = "is_system")
    private boolean system = false;
}