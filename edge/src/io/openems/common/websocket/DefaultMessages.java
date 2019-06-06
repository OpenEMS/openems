package io.openems.common.websocket;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.FieldValue;
import io.openems.common.types.NumberFieldValue;
import io.openems.common.types.StringFieldValue;
import io.openems.common.utils.JsonUtils;

public class DefaultMessages {

	/**
	 * <pre>
	 * 	{
	 * 		messageId: {
	 * 			ui: UUID,
	 * 			backend?: UUID
	 *		}
	 * 	}
	 * </pre>
	 *
	 * @param jMessageId
	 * @return
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
	 *			mode: "allow",
	 *			token: String
	 *		}, metadata: {
	 *			edges: [{
	 *				id: number
	 *				name: String,
	 *				comment: String,
	 *				producttype: String,
	 *				role: "admin" | "installer" | "owner" | "guest",
	 *				online: boolean
	 *			}]
	 *		}
	 *	}
	 * </pre>
	 *
	 * @param token
	 * @return
	 */
	public static JsonObject uiLoginSuccessfulReply(String token, JsonArray jEdges) {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "allow");
		jAuthenticate.addProperty("token", token);
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		JsonObject jMetadata = new JsonObject();
		jMetadata.add("edges", jEdges);
		j.add("metadata", jMetadata);
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
	 * @param token
	 * @return
	 */
	@Deprecated
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
	 * @param token
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
	 *		timedata: {
	 *			timestamp (Long): {
	 *				channel: String,
	 *				value: String | Number
	 *			}
	 *		}
	 *	}
	 * </pre>
	 *
	 * @param token
	 * @return
	 */
	public static JsonObject timestampedData(long timestamp, HashMap<ChannelAddress, FieldValue<?>> queue) {
		JsonObject jTimestamp = new JsonObject();
		for (Entry<ChannelAddress, FieldValue<?>> entry : queue.entrySet()) {
			String address = entry.getKey().toString();
			FieldValue<?> fieldValue = entry.getValue();
			if (fieldValue instanceof NumberFieldValue) {
				jTimestamp.addProperty(address, ((NumberFieldValue) fieldValue).value);
			} else if (fieldValue instanceof StringFieldValue) {
				jTimestamp.addProperty(address, ((StringFieldValue) fieldValue).value);
			}
		}
		JsonObject jTimedata = new JsonObject();
		jTimedata.add(String.valueOf(timestamp), jTimestamp);
		JsonObject j = new JsonObject();
		j.add("timedata", jTimedata);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		config: {
	 *			...
	 *		}
	 *	}
	 * </pre>
	 *
	 * @param token
	 * @return
	 */
	public static JsonObject configQueryReply(JsonObject jMessageId, JsonObject config) {
		JsonObject j = newMessage(jMessageId);
		j.add("config", config);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		currentData: {[{
	 *			channel: string,
	 *			value: any
	 *		}]}
	 *	}
	 * </pre>
	 *
	 * @return
	 */
	public static JsonObject currentData(JsonObject jMessageId, JsonObject jCurrentData) {
		JsonObject j = newMessage(jMessageId);
		j.add("currentData", jCurrentData);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		historicData: {
	 *			data: [{
	 *				time: ...,
	 *				channels: {
	 *					thing: {
	 *						channel: any
	 *					}
	 *				}
	 *			}]
	 *		}
	 *	}
	 * </pre>
	 *
	 * @return
	 */
	public static JsonObject historicDataQueryReply(JsonObject jMessageId, JsonArray jData) {
		JsonObject j = newMessage(jMessageId);
		JsonObject jHistoricData = new JsonObject();
		jHistoricData.add("data", jData);
		j.add("historicData", jHistoricData);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		notification: {
	 *			type: string,
	 *			message: string,
	 *			code: number,
	 *			params: string[]
	 *		}
	 *	}
	 * </pre>
	 *
	 * @return
	 */
	public static JsonObject notification(JsonObject jMessageId, Notification code, String message, Object... params) {
		JsonObject j = newMessage(jMessageId);
		JsonObject jNotification = new JsonObject();
		jNotification.addProperty("type", code.getType().toString().toLowerCase());
		jNotification.addProperty("message", message);
		jNotification.addProperty("code", code.getValue());
		JsonArray jParams = new JsonArray();
		for (Object param : params) {
			jParams.add(param.toString());
		}
		jNotification.add("params", jParams);
		j.add("notification", jNotification);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {}
	 *		currentData: {
	 *			mode: 'subscribe',
	 *			channels: {}
	 *		}
	 *	}
	 * </pre>
	 *
	 * @return
	 */
	@Deprecated
	public static JsonObject currentDataSubscribe(JsonObject jMessageId, JsonObject jChannels) {
		JsonObject j = newMessage(jMessageId);
		JsonObject jCurrentData = new JsonObject();
		jCurrentData.addProperty("mode", "subscribe");
		jCurrentData.add("channels", jChannels);
		j.add("currentData", jCurrentData);
		return j;
	}

	/**
	 * <pre>
	 *	{
	 *		messageId: {},
	 *		log: {
	 *			times: number,
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
	 *		log: {
	 *			mode: "unsubscribe"
	 *		}
	 *	}
	 * </pre>
	 *
	 * @return
	 */
	public static JsonObject logUnsubscribe(JsonObject jMessageId) {
		JsonObject j = newMessage(jMessageId);
		JsonObject jLog = new JsonObject();
		jLog.addProperty("mode", "unsubscribe");
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

	/**
	 * Adds the backend identifier to messageId. Used for forwarding UI-messages to
	 * Edge
	 *
	 * @param jMessage
	 * @param uuid
	 * @return
	 * @throws OpenemsException
	 */
	@Deprecated
	public static JsonObject prepareMessageForForwardToEdge(JsonObject jMessage, UUID uuid, Optional<Role> roleOpt)
			throws OpenemsException {
		JsonObject jMessageId = JsonUtils.getAsJsonObject(jMessage, "messageId");
		jMessageId.addProperty("backend", uuid.toString());
		jMessage.add("messageId", jMessageId);
		jMessage.remove("edgeId");
		if (roleOpt.isPresent()) {
			jMessage.addProperty("role", roleOpt.get().toString().toLowerCase());
		}
		return jMessage;
	}

	@Deprecated
	public static JsonObject prepareMessageForForwardToUi(JsonObject jMessage) throws OpenemsException {
		JsonObject jMessageId = JsonUtils.getAsJsonObject(jMessage, "messageId");
		jMessageId.remove("backend");
		jMessage.add("messageId", jMessageId);
		return jMessage;
	}
}
