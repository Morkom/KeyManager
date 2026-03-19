package dev.morkom.keymanager.dto;

import dev.morkom.keymanager.model.Role;
import java.util.Set;

public record CreateUserRequest(
    String username,
    String password,
    Set<Role> roles
) {}
