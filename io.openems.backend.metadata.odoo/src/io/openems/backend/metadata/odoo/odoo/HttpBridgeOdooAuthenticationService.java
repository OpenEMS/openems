package io.openems.backend.metadata.odoo.odoo;

import java.util.concurrent.CompletableFuture;

import com.google.common.annotations.VisibleForTesting;

import io.openems.backend.metadata.odoo.odoo.http.OdooResponseError;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationServiceConfig;
import io.openems.common.utils.JsonUtils;

public class HttpBridgeOdooAuthenticationService
		implements HttpBridgeService, HttpBridgeAuthenticationServiceConfig<String> {

	private final Credentials credentials;
	private final BridgeHttp bridgeHttp;
	private final OdooHandler odooHandler;

	public HttpBridgeOdooAuthenticationService(Credentials credentials, BridgeHttp bridgeHttp,
			OdooHandler odooHandler) {
		this.credentials = credentials;
		this.bridgeHttp = bridgeHttp;
		this.odooHandler = odooHandler;
	}

	@Override
	public CompletableFuture<String> fetchAuthHeader() {
		return this.odooHandler.authenticateAsAdmin();
	}

	@Override
	public BridgeHttp.Endpoint applyAuthentication(BridgeHttp.Endpoint endpoint, String sessionId) {
		return endpoint.toBuilder() //
				.setHeader(HttpHeader.cookie("session_id=" + sessionId)) //
				.build();
	}

	@Override
	public boolean isSessionExpired(HttpResponse<String> response, Throwable throwable) {
		final var json = JsonUtils.parseOptional(response.data());

		return json.flatMap(JsonUtils::getAsOptionalJsonObject) //
				.map(j -> j.get("error")) //
				.map(OdooResponseError.serializer()::deserialize) //
				.map(error -> "odoo.http.SessionExpiredException".equals(error.dataName())) //
				.orElse(false);
	}

	/**
	 * Logs in as the admin user and returns a CompletableFuture that will complete
	 * with the session ID.
	 *
	 * @return a CompletableFuture with the session ID
	 */
	@VisibleForTesting
	CompletableFuture<String> loginAsAdmin() {
		return this.login(this.credentials.login(), this.credentials.password());
	}

	private CompletableFuture<String> login(String login, String password) {
		return this.bridgeHttp
				.requestJson(BridgeHttp.Endpoint.create(this.credentials.url() + "/web/session/authenticate")
						.setBodyJson(JsonUtils.buildJsonObject() //
								.addProperty("jsonrpc", "2.0") //
								.addProperty("method", "call") //
								.add("params", JsonUtils.buildJsonObject() //
										.addProperty("db", this.credentials.database()) //
										.addProperty("login", login.toLowerCase()) //
										.addProperty("password", password) //
										.build()) //
								.build())
						.build()) //
				.thenApply(response -> {
					return OdooUtils.getFieldFromSetCookieHeader(response.header(), "session_id")
							.orElseThrow(() -> new AccessDeniedException(
									"Missing 'session_id' in response 'set-cookie' header. Response: %s"
											.formatted(response)));
				});
	}

	@Override
	public void close() {
		// empty
	}

}
