package io.openems.edge.controller.api.backend.dummy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.osgi.service.event.Event;

import io.openems.common.event.EventEmitter;
import io.openems.common.event.Subscribable;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.backend.api.BackendEvent;
import io.openems.edge.controller.api.backend.api.BackendFeature;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

public class DummyControllerApiBackend extends AbstractDummyOpenemsComponent<DummyControllerApiBackend>
		implements ControllerApiBackend {

	private boolean connected = false;
	private final EventEmitter<BackendEvent> eventEmitter = new EventEmitter<>();
	private Function<JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> requestHandler = //
			request -> CompletableFuture
					.failedFuture(OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod()));

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
		return this.requestHandler.apply(request);
	}

	@Override
	public Subscribable<BackendEvent> getEventHandler() {
		return this.eventEmitter;
	}

	@Override
	public BackendFeature[] getSupportedFeatures() {
		return new BackendFeature[0];
	}

	@Override
	public void run() {
		// Nothing here
	}

	@Override
	public void handleEvent(Event event) {
		// Nothing here
	}

	/**
	 * Sends the given event to all EventEmitter subscribers.
	 * 
	 * @param event Event to send
	 */
	public void emitBackendEvent(BackendEvent event) {
		this.eventEmitter.emit(event);
	}

	/**
	 * Simulates the receiving of the specified notification.
	 * 
	 * @param notification The notification to simulate receiving
	 */
	public void simulateNotificationReceive(JsonrpcNotification notification) {
		this.emitBackendEvent(new BackendEvent.Notification(notification));
	}

	public void setRequestHandler(
			Function<JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> requestHandler) {
		this.requestHandler = requestHandler;
	}
}
