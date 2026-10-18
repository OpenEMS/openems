package io.openems.edge.controller.api.backend.api;

public class NoBackendAvailableException extends RuntimeException {
	public NoBackendAvailableException() {
		super("No backend available");
	}
}
