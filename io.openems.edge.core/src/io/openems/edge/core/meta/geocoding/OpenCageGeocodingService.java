package io.openems.edge.core.meta.geocoding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.UrlBuilder;

public class OpenCageGeocodingService {

	private static final String API_SCHEME = "https";
	private static final String API_HOST = "api.opencagedata.com";
	private static final String API_VERSION = "v1";
	private static final String API_FORMAT = "json";

	/**
	 * Specifies the number of results to return.
	 */
	private static final int LIMIT = 2;

	/**
	 * Indicates whether annotations should not be included in the response.
	 */
	private static final boolean NO_ANNOTATIONS = false;

	/**
	 * Indicates whether the query contents should not be logged for privacy
	 * reasons.
	 */
	private static final boolean NO_RECORD = true;

	private final BridgeHttp httpBridge;
	private final String apiKey;
	private final UrlBuilder baseUrl;

	public OpenCageGeocodingService(BridgeHttp httpBridge, String apiKey) {
		this.httpBridge = httpBridge;
		this.apiKey = apiKey;
		this.baseUrl = this.buildBaseUrl();
	}

	/**
	 * Gets the geographic coordinates (latitude and longitude) for a given
	 * location.
	 *
	 * @param location the location name or address to look up
	 * @return a CompletableFuture containing the coordinates for the location, or
	 *         completed exceptionally if the input or API key is invalid
	 */
	public CompletableFuture<List<GeoResult>> geocode(String location) {
		if (location == null || location.isBlank()) {
			return CompletableFuture.failedFuture(new IllegalArgumentException("Location must not be null or empty"));
		}

		if (this.apiKey == null || this.apiKey.isBlank()) {
			return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid API key"));
		}

		final var url = this.baseUrl//
				.withQueryParam("q", location)//
				.toEncodedString();

		return this.httpBridge.getJson(url).thenApply(//
				response -> GeoResult.fromOpenCageApiJsonDeserializer().toListSerializer()
						.deserialize(response.data().getAsJsonObject().get("results"))//
		);
	}

	private UrlBuilder buildBaseUrl() {
		return UrlBuilder.create()//
				.withScheme(API_SCHEME)//
				.withHost(API_HOST)//
				.withPath("/geocode/" + API_VERSION + "/" + API_FORMAT)//
				.withQueryParam("key", this.apiKey)//
				.withQueryParam("limit", String.valueOf(LIMIT))//
				.withQueryParam("no_annotations", NO_ANNOTATIONS ? "1" : "0")//
				.withQueryParam("no_record", NO_RECORD ? "1" : "0");
	}
}
