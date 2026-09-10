package com.github.escom.escomvoting.model.dto;

public record UpdateProfileRequest(
        String name,
        String currentPassword,
        String newPassword,
        Boolean admin
) {}
