package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.WsData;

public class TestClient extends AbstractWebsocketClient {

	private Logger log = LoggerFactory.getLogger(TestClient.class);

	protected TestClient(URI serverUri) {
		super(serverUri);
	}

	@Override
	protected WsData onOpen(JsonObject handshake) {
		log.info("OnOpen: " + handshake);
		return new WsData();
	}

	@Override
	protected void onRequest(JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		log.info("OnRequest: " + request);
	}

	@Override
	protected void onError(Exception ex) throws OpenemsException {
		log.info("onError: " + ex.getMessage());
	}

	@Override
	protected void onClose(int code, String reason, boolean remote) throws OpenemsException {
		log.info("onClose: " + reason);
	}

	@Override
	protected void onInternalError(Exception ex) {
		log.warn("onInternalError: " + ex.getMessage());
	}

}
