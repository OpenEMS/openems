package io.openems.edge.app.api;

import java.net.URI;

import io.openems.edge.app.common.props.CommonProps;

final class CleverPvUrl {

	public static boolean isNewUrl(String url) {
		return url != null && !CommonProps.MASKED_PASSWORD.equals(url);
	}

	public static boolean isValid(String url) {
		if (url == null) {
			return false;
		}
		final URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException e) {
			return false;
		}

		final var query = uri.getRawQuery();
		if (query == null) {
			return false;
		}
		for (var parameter : query.split("&")) {
			final var parts = parameter.split("=", 2);
			if (parts.length == 2 && "code".equals(parts[0]) && !parts[1].isBlank()) {
				return true;
			}
		}
		return false;
	}

	private CleverPvUrl() {
	}
}
