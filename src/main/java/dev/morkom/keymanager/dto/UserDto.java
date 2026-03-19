package dev.morkom.keymanager.dto;

import dev.morkom.keymanager.model.Role;
import java.util.Set;

public record UserDto(
    Long id,
    String username,
    Set<Role> roles
) {}
