package io.openems.edge.bridge.http.api;

public enum HttpMethod {
	GET(false), //
	POST(true), //
	PUT(true), //
	DELETE(false), //
	PATCH(true), //
	HEAD(false), //
	OPTIONS(false), //
	TRACE(false) //
	;

	private final boolean bodyAllowed;

	private HttpMethod(boolean bodyAllowed) {
		this.bodyAllowed = bodyAllowed;
	}

	public boolean isBodyAllowed() {
		return this.bodyAllowed;
	}

}
