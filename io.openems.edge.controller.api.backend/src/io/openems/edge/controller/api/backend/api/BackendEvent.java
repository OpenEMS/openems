package io.openems.edge.controller.api.backend.api;

import io.openems.common.jsonrpc.base.JsonrpcNotification;

public sealed interface BackendEvent {
	record BackendConnectionEstablishedEvent() implements BackendEvent {
	}

	record Notification(JsonrpcNotification notification) implements BackendEvent {
	}
}
