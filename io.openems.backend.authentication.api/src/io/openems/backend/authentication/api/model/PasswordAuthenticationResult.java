package io.openems.backend.authentication.api.model;

public record PasswordAuthenticationResult(String userId, String login, String token) {
}
