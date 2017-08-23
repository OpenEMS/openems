package io.openems.common.websocket;

import com.google.gson.JsonObject;

public class DefaultMessages {

	/**
	 * <pre>
	 *	{
	 *		authenticate: {
	 *			mode: "allow",
	 *			token: String
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @param token
	 * @return
	 */
	public static JsonObject connectionSuccessfulReply(String token) {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "allow");
		jAuthenticate.addProperty("token", token);
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		return j;
	}
	
	/**
	 * <pre>
	 *	{
	 *		authenticate: {
	 *			mode: "deny"
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @param token
	 * @return
	 */
	public static JsonObject connectionFailedReply() {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "deny");
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		return j;
	}
}
