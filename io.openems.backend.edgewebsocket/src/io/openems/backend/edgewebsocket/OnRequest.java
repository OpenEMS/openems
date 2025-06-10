package io.openems.backend.edgewebsocket;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.AppCenterHandler;
import io.openems.backend.common.metadata.AppCenterMetadata;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AppCenterRequest;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final Supplier<AppCenterMetadata.EdgeData> appCenterMetadata;
	private final BiConsumer<Logger, String> logWarn;

	public OnRequest(//
			Supplier<AppCenterMetadata.EdgeData> appCenterMetadata, //
			BiConsumer<Logger, String> logWarn) {
		this.appCenterMetadata = appCenterMetadata;
		this.logWarn = logWarn;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {

		final WsData wsData = ws.getAttachment();
		final var edgeId = wsData.getEdgeId().get();

		CompletableFuture<? extends JsonrpcResponseSuccess> resultFuture = null;

		switch (request.getMethod()) {
		case AppCenterRequest.METHOD:
			resultFuture = AppCenterHandler.handleEdgeRequest(this.appCenterMetadata.get(), //
					AppCenterRequest.from(request), edgeId);
			break;

		}

		if (resultFuture != null) {
			return resultFuture;
		}
		this.logWarn.accept(this.log, "Unhandled Request: " + request);
		throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
	}

}
