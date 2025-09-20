package io.openems.edge.controller.api.websocket;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
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
import io.openems.edge.common.jsonapi.Key;
import io.openems.edge.common.jsonapi.SingleJsonApiBinder;
import io.openems.edge.controller.api.websocket.handler.RootRequestHandler;

@Component(service = { OnRequest.class }, scope = ServiceScope.PROTOTYPE)
public class OnRequest implements io.openems.common.websocket.OnRequest {

	@Component(service = OnRequest.Factory.class)
	public static class Factory {

		@Reference
		private ComponentServiceObjects<OnRequest> cso;

		/**
		 * Returns a new {@link OnRequest} service object.
		 * 
		 * @return the created {@link OnRequest} object
		 * @see #unget(OnRequest)
		 */
		public OnRequest get() {
			return this.cso.getService();
		}

		/**
		 * Releases the {@link OnRequest} service object.
		 * 
		 * @param service a {@link OnRequest} provided by this factory
		 * @see #get()
		 */
		public void unget(OnRequest service) {
			if (service == null) {
				return;
			}
			this.cso.ungetService(service);
		}

	}

	public static final Key<WsData> WS_DATA_KEY = new Key<>("wsData", WsData.class);
	public static final Key<WebSocket> WEBSOCKET_KEY = new Key<>("websocket", WebSocket.class);

	private final SingleJsonApiBinder apiBinder = new SingleJsonApiBinder();
	private Consumer<Call<JsonrpcRequest, JsonrpcResponse>> onCall = FunctionUtils::doNothing;

	@Reference(//
			cardinality = ReferenceCardinality.OPTIONAL, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	protected void bindRootHandler(RootRequestHandler rootHandler) {
		this.apiBinder.bind(rootHandler);
	}

	protected void unbindRootHandler(RootRequestHandler rootHandler) {
		this.apiBinder.unbind();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		return this.apiBinder.handleRequest(request, call -> {
			WsData wsData = ws.getAttachment();
			call.put(WS_DATA_KEY, wsData);
			wsData.getUser().ifPresent(user -> {
				call.put(EdgeKeys.USER_KEY, user);
			});
			this.onCall.accept(call);
		});
	}

	public void setOnCall(Consumer<Call<JsonrpcRequest, JsonrpcResponse>> callAction) {
		this.onCall = callAction == null ? FunctionUtils::doNothing : callAction;
	}

	public void setDebug(boolean debug) {
		this.apiBinder.setDebug(debug);
	}

}
