package io.openems.common.session;

public class Session<D extends SessionData> {
	private final String token;
	private boolean valid = false;
	
	/**
	 * store additional metadata to this session
	 */
	private final D data;

	protected Session(String token, D data) {
		this.token = token;
		this.data = data;
	}

	public String getToken() {
		return token;
	}
	
	public void setValid() {
		this.valid = true;
	}

	public void setInvalid() {
		this.valid = false;
	}
	
	public boolean isValid() {
		return this.valid;
	}
	
	public D getData() {
		return data;
	}

	@Override
	public String toString() {
		return "Session [token=" + token + ", valid=" + valid + ", data=" + data + "]";
	}
}
