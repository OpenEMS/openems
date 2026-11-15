package io.openems.edge.common.test;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;

public final class RpcTestUtils {

	/**
	 * Registers the routes of the {@link ComponentJsonApiExtension} for the given
	 * component and sends a request without parameters.
	 *
	 * @param <T>       the type of the extended {@link ComponentJsonApi}
	 * @param extension the {@link ComponentJsonApiExtension} providing the endpoint
	 * @param service   the component the extension is applied to
	 * @param method    the method of the request
	 * @param user      the {@link User} who sends the request
	 * @return the handled {@link Call}
	 */
	public static <T extends ComponentJsonApi> Call<JsonrpcRequest, JsonrpcResponse> performEmptyCall(//
			ComponentJsonApiExtension<T> extension, //
			T service, //
			String method, //
			User user //
	) {
		return performCall(extension, service, method, JsonUtils.buildJsonObject().build(), user);
	}

	/**
	 * Registers the routes of the {@link ComponentJsonApiExtension} for the given
	 * component and sends a request of the given {@link EndpointRequestType}.
	 *
	 * @param <T>         the type of the extended {@link ComponentJsonApi}
	 * @param <REQUEST>   the type of the request
	 * @param <RESPONSE>  the type of the response
	 * @param extension   the {@link ComponentJsonApiExtension} providing the
	 *                    endpoint
	 * @param service     the component the extension is applied to
	 * @param requestType the {@link EndpointRequestType} defining method and
	 *                    serializer
	 * @param request     the request object
	 * @param user        the {@link User} who sends the request
	 * @return the handled {@link Call}
	 */
	public static <T extends ComponentJsonApi, REQUEST, RESPONSE> Call<JsonrpcRequest, JsonrpcResponse> performCall(//
			ComponentJsonApiExtension<T> extension, //
			T service, //
			EndpointRequestType<REQUEST, RESPONSE> requestType, //
			REQUEST request, //
			User user //
	) {
		return performCall(//
				extension, //
				service, //
				requestType.getMethod(), //
				requestType.getRequestSerializer().serialize(request).getAsJsonObject(), //
				user//
		);
	}

	private static <T extends ComponentJsonApi> Call<JsonrpcRequest, JsonrpcResponse> performCall(//
			ComponentJsonApiExtension<T> extension, //
			T service, //
			String method, //
			JsonObject params, //
			User user //
	) {
		var builder = new JsonApiBuilder();
		extension.buildJsonApiRoutes(service, builder);
		var call = new Call<JsonrpcRequest, JsonrpcResponse>(new GenericJsonrpcRequest(method, params));
		call.put(EdgeKeys.USER_KEY, user);
		builder.handle(call);
		return call;
	}

	private RpcTestUtils() {
	}

}
