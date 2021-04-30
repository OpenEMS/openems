package io.openems.backend.metadata.odoo.odoo;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.OdooMetadata;
import io.openems.backend.metadata.odoo.odoo.OdooUtils.SuccessResponseAndHeaders;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OdooHandler {

	protected final OdooMetadata parent;

	private final Logger log = LoggerFactory.getLogger(OdooHandler.class);
	private final Credentials credentials;

	public OdooHandler(OdooMetadata parent, Config config) {
		this.parent = parent;
		this.credentials = Credentials.fromConfig(config);
	}

	/**
	 * Writes one field to Odoo Edge model.
	 * 
	 * @param edge        the Edge
	 * @param fieldValues the FieldValues
	 */
	public void writeEdge(MyEdge edge, FieldValue<?>... fieldValues) {
		try {
			OdooUtils.write(this.credentials, Field.EdgeDevice.ODOO_MODEL, new Integer[] { edge.getOdooId() },
					fieldValues);
		} catch (OpenemsException e) {
			this.parent.logError(this.log, "Unable to update Edge [" + edge.getId() + "] " //
					+ "Odoo-ID [" + edge.getOdooId() + "] " //
					+ "Fields [" + Stream.of(fieldValues).map(v -> v.toString()).collect(Collectors.joining(","))
					+ "]: " + e.getMessage());
		}
	}

	/**
	 * Adds a message in Odoo Chatter ('mail.thread').
	 * 
	 * @param edge    the Edge
	 * @param message the message
	 */
	public void addChatterMessage(MyEdge edge, String message) {
		try {
			OdooUtils.addChatterMessage(this.credentials, Field.EdgeDevice.ODOO_MODEL, edge.getOdooId(), message);
		} catch (OpenemsException e) {
			this.parent.logError(this.log, "Unable to add Chatter Message to Edge [" + edge.getId() + "] " //
					+ "Message [" + message + "]" //
					+ ": " + e.getMessage());
		}
	}

	/**
	 * Authenticates a user using Username and Password.
	 * 
	 * @param username the Username
	 * @param password the Password
	 * @return the session_id
	 * @throws OpenemsNamedException on login error
	 */
	public String authenticate(String username, String password) throws OpenemsNamedException {
		JsonObject request = JsonUtils.buildJsonObject() //
				.addProperty("jsonrpc", "2.0") //
				.addProperty("method", "call") //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("db", "v12") //
						.addProperty("login", username) //
						.addProperty("password", password) //
						.build()) //
				.build();
		SuccessResponseAndHeaders response = OdooUtils
				.sendJsonrpcRequest(this.credentials.getUrl() + "/web/session/authenticate", request);
		Optional<String> sessionId = getFieldFromSetCookieHeader(response.headers, "session_id");
		if (!sessionId.isPresent()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		} else {
			return sessionId.get();
		}
	}

	/**
	 * Authenticates a user using a Session-ID.
	 * 
	 * @param sessionId the Odoo Session-ID
	 * @return the {@link JsonObject} received from /openems_backend/info.
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject authenticateSession(String sessionId) throws OpenemsNamedException {
		return JsonUtils
				.getAsJsonObject(OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/openems_backend/info",
						"session_id=" + sessionId, new JsonObject()).result);
	}

	/**
	 * Logout a User.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void logout(String sessionId) {
		try {
			OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/web/session/destroy", "session_id=" + sessionId,
					new JsonObject());
		} catch (OpenemsNamedException e) {
			this.log.warn("Unable to logout session [" + sessionId + "]: " + e.getMessage());
		}
	}

	/**
	 * Get field from the 'Set-Cookie' field in HTTP headers.
	 * 
	 * <p>
	 * Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * all variants of 'cookie' are accepted.
	 * 
	 * @param headers   the HTTP headers
	 * @param fieldname the field name
	 * @return value as optional
	 */
	public static Optional<String> getFieldFromSetCookieHeader(Map<String, List<String>> headers, String fieldname) {
		for (Entry<String, List<String>> header : headers.entrySet()) {
			String key = header.getKey();
			if (key != null && key.equalsIgnoreCase("Set-Cookie")) {
				for (String cookie : header.getValue()) {
					for (String cookieVariable : cookie.split("; ")) {
						String[] keyValue = cookieVariable.split("=");
						if (keyValue.length == 2) {
							if (keyValue[0].equals(fieldname)) {
								return Optional.ofNullable(keyValue[1]);
							}
						}
					}
				}
			}
		}
		return Optional.empty();
	}
}
