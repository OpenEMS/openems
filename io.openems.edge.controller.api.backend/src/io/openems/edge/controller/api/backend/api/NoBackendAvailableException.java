package io.openems.edge.controller.api.backend.api;

public class NoBackendAvailableException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoBackendAvailableException() {
		super("No backend available");
	}
}
