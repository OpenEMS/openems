package io.openems.edge.controller.api.rest;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.SingleJsonApiBinder;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.rest.handler.RootRequestHandler;

@Component(//
		scope = ServiceScope.PROTOTYPE, //
		service = JsonRpcRestHandler.class //
)
public class JsonRpcRestHandler {

	@Component(service = JsonRpcRestHandler.Factory.class)
	public static class Factory {

		@Reference
		private ComponentServiceObjects<JsonRpcRestHandler> cso;

		/**
		 * Returns a new {@link JsonRpcRestHandler} service object.
		 * 
		 * @return the created {@link JsonRpcRestHandler} object
		 * @see #unget(JsonRpcRestHandler)
		 */
		public JsonRpcRestHandler get() {
			return this.cso.getService();
		}

		/**
		 * Releases the {@link JsonRpcRestHandler} service object.
		 * 
		 * @param service a {@link JsonRpcRestHandler} provided by this factory
		 * @see #get()
		 */
		public void unget(JsonRpcRestHandler service) {
			if (service == null) {
				return;
			}
			this.cso.ungetService(service);
		}

	}

	private final SingleJsonApiBinder apiBinder = new SingleJsonApiBinder();

	private Consumer<Call<JsonrpcRequest, JsonrpcResponse>> onCall = FunctionUtils::doNothing;

	/**
	 * Binds the {@link RootRequestHandler}.
	 * 
	 * @param rootHandler the handler
	 */
	@Reference(//
			cardinality = ReferenceCardinality.OPTIONAL, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	public void bindRootHandler(RootRequestHandler rootHandler) {
		this.apiBinder.bind(rootHandler);
	}

	/**
	 * Unbinds the {@link RootRequestHandler}.
	 * 
	 * @param rootHandler the handler
	 */
	public void unbindRootHandler(RootRequestHandler rootHandler) {
		this.apiBinder.unbind();
	}

	/**
	 * Handles a rest request.
	 * 
	 * @param user    the user of the current request
	 * @param request the request to handle
	 * @return the result future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		return this.apiBinder.handleRequest(request, call -> {
			call.put(EdgeKeys.USER_KEY, user);
			this.onCall.accept(call);
		});
	}

	public void setOnCall(Consumer<Call<JsonrpcRequest, JsonrpcResponse>> onCall) {
		this.onCall = onCall == null ? FunctionUtils::doNothing : onCall;
	}

}
