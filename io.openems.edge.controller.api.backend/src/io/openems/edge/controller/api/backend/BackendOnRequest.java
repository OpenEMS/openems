package io.openems.edge.controller.api.backend;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.websocket.OnRequest;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.SingleJsonApiBinder;
import io.openems.edge.controller.api.backend.handler.RootRequestHandler;

@Component(//
		scope = ServiceScope.PROTOTYPE, //
		service = { BackendOnRequest.class, OnRequest.class } //
)
public class BackendOnRequest implements OnRequest {

	@Component(service = BackendOnRequest.Factory.class)
	public static class Factory {

		@Reference
		private ComponentServiceObjects<BackendOnRequest> cso;

		/**
		 * Returns a new {@link BackendOnRequest} service object.
		 * 
		 * @return the created {@link BackendOnRequest} object
		 * @see #unget(BackendOnRequest)
		 */
		public BackendOnRequest get() {
			return this.cso.getService();
		}

		/**
		 * Releases the {@link BackendOnRequest} service object.
		 * 
		 * @param service a {@link BackendOnRequest} provided by this factory
		 * @see #get()
		 */
		public void unget(BackendOnRequest service) {
			if (service == null) {
				return;
			}
			this.cso.ungetService(service);
		}

	}

	private final SingleJsonApiBinder apiBinder = new SingleJsonApiBinder();
	private Consumer<Call<JsonrpcRequest, JsonrpcResponse>> onCall = FunctionUtils::doNothing;

	@Activate
	public BackendOnRequest(@Reference RootRequestHandler handler) {
		this.apiBinder.bind(handler);
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(//
			final WebSocket ws, //
			final JsonrpcRequest request //
	) throws OpenemsNamedException {
		return this.apiBinder.handleRequest(request, this.onCall);
	}

	public void setOnCall(Consumer<Call<JsonrpcRequest, JsonrpcResponse>> callAction) {
		this.onCall = callAction == null ? FunctionUtils::doNothing : callAction;
	}

	public void setDebug(boolean debug) {
		this.apiBinder.setDebug(debug);
	}

}
