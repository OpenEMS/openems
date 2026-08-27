package io.openems.backend.metadata.odoo.odoo;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.backend.metadata.odoo.odoo.http.OdooDeviceData;
import io.openems.backend.metadata.odoo.odoo.http.OdooGetEdgeWithRoleRequest;
import io.openems.backend.metadata.odoo.odoo.http.OdooGetEdgesRequest;
import io.openems.backend.metadata.odoo.odoo.http.OdooGetEdgesResponse;
import io.openems.backend.metadata.odoo.odoo.http.OdooGetUserInfoRequest;
import io.openems.backend.metadata.odoo.odoo.http.OdooGetUserInfoResponse;
import io.openems.backend.metadata.odoo.odoo.http.OdooResponseError;
import io.openems.backend.metadata.odoo.odoo.http.OdooSetEdgeSettingsRequest;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class HttpBridgeOdooService implements HttpBridgeService {

	private static final int DEFAULT_READ_TIMEOUT = 5_000;

	private final Credentials credentials;
	private final BridgeHttp bridgeHttp;

	public HttpBridgeOdooService(Credentials credentials, BridgeHttp bridgeHttp) {
		this.credentials = credentials;
		this.bridgeHttp = bridgeHttp;
	}

	/**
	 * Fetches the edges for the given user and pagination options.
	 * 
	 * @param user              the user for which to fetch the edges
	 * @param paginationOptions the pagination options to use for fetching the edges
	 * @return a {@link CompletableFuture} that will complete with a
	 *         {@link OdooGetEdgesResponse}
	 */
	public CompletableFuture<OdooGetEdgesResponse> getEdges(User user,
			GetEdgesRequest.PaginationOptions paginationOptions) {
		final var request = new OdooGetEdgesRequest(user.getUserId(), paginationOptions.getPage(),
				paginationOptions.getLimit(), paginationOptions.getQuery(),
				OdooGetEdgesRequest.SearchParams.from(paginationOptions.getSearchParams()));
		return this.sendRequest("/openems_backend/get_edges", OdooGetEdgesRequest.serializer(),
				OdooGetEdgesResponse.serializer(), request);
	}

	/**
	 * Fetches the edge with the given edgeId for the given user.
	 *
	 * @param request the request containing the edgeId and user for which to fetch
	 *                the edge with role
	 * @return a {@link CompletableFuture} that will complete with a
	 *         {@link OdooDeviceData}
	 */
	public CompletableFuture<OdooDeviceData> getEdgeWithRole(OdooGetEdgeWithRoleRequest request) {
		return this.sendRequest("/openems_backend/get_edge_with_role", OdooGetEdgeWithRoleRequest.serializer(),
				OdooDeviceData.serializer(), request);
	}

	/**
	 * Updates the settings of a edge.
	 *
	 * @param edgeId   the edge id
	 * @param settings the settings of the user
	 * @return a {@link CompletableFuture} that will complete with a
	 *         {@link EmptyObject}
	 */
	public CompletableFuture<Void> updateEdgeSettings(String edgeId, JsonObject settings) {
		return this.sendRequest("/openems_backend/set_edge_settings", OdooSetEdgeSettingsRequest.serializer(),
				new OdooSetEdgeSettingsRequest(edgeId, settings));
	}

	/**
	 * Fetches the user info for the given user.
	 * 
	 * @param request the request containing the user for which to fetch the user
	 *                info
	 * @return a {@link CompletableFuture} that will complete with a
	 *         {@link OdooGetUserInfoResponse}
	 */
	public CompletableFuture<OdooGetUserInfoResponse> getUserInfo(OdooGetUserInfoRequest request) {
		return this.sendRequest("/openems_backend/info", OdooGetUserInfoRequest.serializer(),
				OdooGetUserInfoResponse.serializer(), request);
	}

	private CompletableFuture<HttpResponse<JsonElement>> sendRequest(String path, JsonElement body) {
		return this.bridgeHttp.requestJson(BridgeHttp.Endpoint.create(this.credentials.url() + path) //
				.setConnectTimeout(5_000) //
				.setReadTimeout(DEFAULT_READ_TIMEOUT) //
				.setHeader("Accept-Charset", "US-ASCII") //
				.setBodyJson(body) //
				.build()).thenApply(response -> {

					final var json = response.data().getAsJsonObject();

					// Handle Success or Error
					if (json.has("error")) {
						final var error = OdooResponseError.serializer().deserialize(json.get("error"));

						switch (error.dataName()) {
						case "odoo.exceptions.AccessDenied":
							throw new AccessDeniedException();

						case "odoo.http.SessionExpiredException":
							throw new SessionExpiredException();

						default:
							// for OpenemsExceptions from Odoo only throw OpenemsException with message for
							// more readability
							if (error.dataName().endsWith("OpenemsException")) {
								throw new OpenemsRuntimeException(error.dataMessage());
							}

							var exception = "Exception for Request [" + body + "] to URL [" + path + "]: " //
									+ error.dataMessage() + ";" //
									+ " Code [" + error.code() + "]" //
									+ " Message [" + error.message() + "]" //
									+ " Name [" + error.dataName() + "]" //
									+ " ExceptionType [" + error.dataExceptionType() + "]" //
									+ " Arguments [" + error.dataArguments() + "]" //
									+ " Debug [" + error.dataDebug() + "]";
							throw new OpenemsRuntimeException(exception);
						}
					} else if (json.has("result")) {
						return response.withData(json.get("result"));

					} else {
						// JSON-RPC response by Odoo on /logout is {jsonrpc:2.0, id:null} - without
						// 'result' attribute
						return response;
					}
				});
	}

	private <REQUEST, RESPONSE> CompletableFuture<RESPONSE> sendRequest(String path,
			JsonSerializer<REQUEST> requestJsonSerializer, JsonSerializer<RESPONSE> responseJsonSerializer,
			REQUEST request) {
		return this.sendRequest(path, //
				JsonUtils.buildJsonObject() //
						.add("params", requestJsonSerializer.serialize(request)) //
						.build())
				.thenApply(response -> responseJsonSerializer.deserialize(response.data()));
	}

	private <REQUEST> CompletableFuture<Void> sendRequest(String path, JsonSerializer<REQUEST> requestJsonSerializer,
			REQUEST request) {
		return this.sendRequest(path, requestJsonSerializer, EmptyObject.serializer(), request) //
				.thenAccept(e -> {
					// empty
				});
	}

	@Override
	public void close() {
		// empty
	}

}
