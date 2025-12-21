package io.openems.edge.controller.api.backend.dummy;

import java.util.concurrent.CompletableFuture;

import org.osgi.service.event.Event;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

public class DummyControllerApiBackend extends AbstractDummyOpenemsComponent<DummyControllerApiBackend>
		implements ControllerApiBackend {

	private boolean connected = false;

	public DummyControllerApiBackend(String id) {
		this(id, false);
	}

	public DummyControllerApiBackend(String id, boolean connected) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ControllerApiBackend.ChannelId.values());
		this.connected = connected;
	}

	public void setConnected(boolean value) {
		this.connected = value;
	}

	@Override
	protected DummyControllerApiBackend self() {
		return this;
	}

	@Override
	public boolean isConnected() {
		return this.connected;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> sendRequest(User user, JsonrpcRequest request) {
		return null;
	}

	@Override
	public void run() {
		// Nothing here
	}

	@Override
	public void handleEvent(Event event) {
		// Nothing here
	}
}
