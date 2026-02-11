package com.demo.auth.entity;

public enum UserRole {
    USER("Utilisateur standard avec accès en lecture"),
    ADMIN("Administrateur avec tous les droits"),
    MODERATOR("Modérateur avec droits de modération"),
    SUPPORT("Support client avec accès limité");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean canModerate() {
        return this == ADMIN || this == MODERATOR;
    }
}