package io.openems.edge.io.phoenixcontact.auth;

public final class PlcNextAuthAndAccessTokenDTO {

	private final String code;
	private final int expiresIn;
	private String accessToken;

	public PlcNextAuthAndAccessTokenDTO(String code, int expiresIn) {
		this.code = code;
		this.expiresIn = expiresIn;
	}

	public String getCode() {
		return this.code;
	}

	public int getExpiresIn() {
		return this.expiresIn;
	}

	public String getAccessToken() {
		return this.accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

}
