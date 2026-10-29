package com.aether.api.dto;

public record CategoryDto(
    String id,
    String parentId,
    String name,
    String kind,
    String color,
    String icon,
    boolean system
) {}
