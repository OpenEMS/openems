package io.openems.edge.timeofusetariff.rabotcharge;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpMediaType;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.oem.OpenemsEdgeOem.OAuthClientRegistration;
import io.openems.common.utils.JsonUtils;

public class RabotChargeApiService {

	private final BridgeHttp httpBridge;
	private final HttpBridgeTimeService timeService;
	private final OAuthClientRegistration clientCredentials;

	protected static final String RABOT_CHARGE_TOKEN_URL = "https://auth.rabot-charge.de/connect/token";
	protected static final String RABOT_PARTNER_API_URL = "https://api.rabot-charge.de/partner/v1";

	public static class RabotApiException extends Exception {
		private static final long serialVersionUID = 1L;
		private final HttpError httpError;

		public RabotApiException(HttpError httpError) {
			super(httpError.getMessage());
			this.httpError = httpError;
		}

		public HttpError getHttpError() {
			return this.httpError;
		}
	}

	public RabotChargeApiService(BridgeHttp httpBridge, OAuthClientRegistration clientCredentials) {
		this.httpBridge = httpBridge;
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		this.clientCredentials = clientCredentials;
	}

	/**
	 * Authenticates with the Rabot Charge API using the Client Credentials flow to
	 * obtain an access token.
	 * 
	 * <p>
	 * This method performs a POST request to the token endpoint with the configured
	 * client ID and client secret. It uses a short delay strategy for retries via
	 * the {@link HttpBridgeTimeService}.
	 * 
	 * @return a {@link CompletableFuture} that completes with the access token as a
	 *         {@link String} upon success, or completes exceptionally with a
	 *         {@link RabotApiException} if the API request fails, or other
	 *         exceptions if parsing fails.
	 */
	public CompletableFuture<String> getPartnerToken() {
		final var endpoint = BridgeHttp.create(RABOT_CHARGE_TOKEN_URL) //
				.setHeader(HttpHeader.accept(HttpMediaType.Application.JSON)) //
				.setBodyFormEncoded(Map.of("grant_type", "client_credentials", //
						"scope", "api:partner", //
						"client_id", this.clientCredentials.clientId(), //
						"client_secret", this.clientCredentials.clientSecret() //
				)) //
				.build();

		return this.httpBridge.requestJson(endpoint).thenApply(response -> {
			try {
				// Parse the access_token directly.
				// to avoid crash if "refresh_token" is missing (Rabot does not send it here).
				return JsonUtils.getAsString(response.data(), "access_token");
			} catch (Exception e) {
				throw new CompletionException("Failed to parse access_token", e);
			}
		});
	}

	/**
	 * Creates a Customer Link.
	 * 
	 * @param redirectUrl The OpenEMS redirect URL
	 * @return The authorization URL to redirect the user to
	 */
	public CompletableFuture<RabotChargeApi.LinkResponse> createCustomerLink(String redirectUrl) {
		final var endpoint = Endpoint.create(RABOT_PARTNER_API_URL + "/customers/link") //
				.setBodyJson(JsonUtils.buildJsonObject() //
						.addProperty("successUrl", redirectUrl) //
						.addProperty("failureUrl", redirectUrl) //
						.addProperty("customerNumberQueryParameterName", "code") //
						.addProperty("authAs", "rabot-charge") //
						.build()) //
				.build();

		return this.httpBridge.requestJson(endpoint)
				.thenApply(response -> RabotChargeApi.LinkResponse.serializer().deserialize(response.data()));
	}

	/**
	 * Fetches contracts for a customer.
	 *
	 * @param customerNumber the customer number
	 * @return the available contracts
	 */
	public CompletableFuture<RabotChargeApi.Contracts> getContracts(String customerNumber) {
		final var endpoint = Endpoint.create(RABOT_PARTNER_API_URL + "/customers/" + customerNumber + "/contracts") //
				.build();

		return this.httpBridge.requestJson(endpoint)
				.thenApply(response -> RabotChargeApi.Contracts.serializer().deserialize(response.data()));
	}

	/**
	 * Fetches all customers linked to this partner account.
	 *
	 * @return a {@link CompletableFuture} containing the
	 *         {@link RabotChargeApi.Customers}
	 */
	public CompletableFuture<RabotChargeApi.Customers> getCustomers() {
		final var endpoint = Endpoint.create(RABOT_PARTNER_API_URL + "/customers") //
				.build();

		return this.httpBridge.requestJson(endpoint)
				.thenApply(response -> RabotChargeApi.Customers.serializer().deserialize(response.data()));
	}

	/**
	 * Fetches cost components for a specific contract.
	 *
	 * @param customerNumber the customer number
	 * @param contractId     the contract id
	 * @return the price components
	 */
	public CompletableFuture<RabotChargeApi.PriceComponents> getCosts(String customerNumber, String contractId) {
		final var endpoint = Endpoint
				.create(RABOT_PARTNER_API_URL + "/customers/" + customerNumber + "/contracts/" + contractId + "/costs") //
				.build();

		return this.httpBridge.requestJson(endpoint)
				.thenApply(response -> RabotChargeApi.PriceComponents.serializer().deserialize(response.data()));
	}

	public HttpBridgeTimeService getTimeService() {
		return this.timeService;
	}

}