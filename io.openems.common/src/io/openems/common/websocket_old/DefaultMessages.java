package io.openems.common.websocket_old;

import com.google.gson.JsonObject;

public class DefaultMessages {

	/**
	 * Creates a new message.
	 * 
	 * <pre>
	 * 	{
	 * 		messageId: {
	 * 			ui: UUID,
	 * 			backend?: UUID
	 *		}
	 * 	}
	 * </pre>
	 * 
	 * @param jMessageId the Message-ID
	 * @return the message
	 */
	@Deprecated
	private static JsonObject newMessage(JsonObject jMessageId) {
		JsonObject j = new JsonObject();
		j.add("messageId", jMessageId);
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
	 * @return
	 */
	public static JsonObject uiLogoutReply() {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "deny");
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		authenticate: {
	 *			mode: "allow"
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @return
	 */
	public static JsonObject openemsConnectionSuccessfulReply() {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "allow");
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		authenticate: {
	 *			mode: "deny",
	 *			message: String
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @param message
	 * @return
	 */
	public static JsonObject openemsConnectionFailedReply(String message) {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "deny");
		jAuthenticate.addProperty("message", message);
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		log: {
	 *			time: number,
	 *			level: string,
	 *			source: string,
	 *			message: string
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @return
	 */
	public static JsonObject log(JsonObject jMessageId, long timestamp, String level, String source, String message) {
		JsonObject j = newMessage(jMessageId);
		JsonObject jLog = new JsonObject();
		jLog.addProperty("time", timestamp);
		jLog.addProperty("level", level);
		jLog.addProperty("source", source);
		jLog.addProperty("message", message);
		j.add("log", jLog);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		system: {
	 *			mode: "executeReply",
	 *			output: string
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @return
	 */
	public static JsonObject systemExecuteReply(JsonObject jMessageId, String output) {
		JsonObject j = newMessage(jMessageId);
		JsonObject jSystem = new JsonObject();
		jSystem.addProperty("mode", "executeReply");
		jSystem.addProperty("output", output);
		j.add("system", jSystem);
		return j;
	}

}
