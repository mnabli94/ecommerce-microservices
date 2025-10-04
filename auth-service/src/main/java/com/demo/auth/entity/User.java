package com.demo.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;


@Entity
@Table(name="users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    UUID id;
    @Column(unique=true, nullable=false)
    String username;
    @Column(nullable=false)
    String passwordHash; // BCrypt
    @Column(nullable=false)
    String roles; // "ROLE_USER,ROLE_ADMIN"

}