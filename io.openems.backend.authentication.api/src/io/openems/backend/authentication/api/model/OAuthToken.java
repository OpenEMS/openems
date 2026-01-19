package io.openems.backend.authentication.api.model;

public record OAuthToken(//
		String sub, //
		String login, //
		String accessToken, //
		String refreshToken //
) {
}
