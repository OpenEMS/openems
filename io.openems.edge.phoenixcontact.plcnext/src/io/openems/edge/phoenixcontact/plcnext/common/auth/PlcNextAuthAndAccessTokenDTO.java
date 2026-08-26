package io.openems.edge.phoenixcontact.plcnext.common.auth;

/**
 * Data transfer object joining result of both auth requests.
 */
//CHECKSTYLE:OFF
public final class PlcNextAuthAndAccessTokenDTO {
	// CHECKSTYLE:ON

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

	@Override
	public String toString() {
		return "PlcNextAuthAndAccessTokenDTO [code=" + this.code + ", expiresIn=" + this.expiresIn + ", accessToken="
				+ this.accessToken + "]";
	}
}
