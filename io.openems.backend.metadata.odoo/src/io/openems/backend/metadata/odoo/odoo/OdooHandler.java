package io.openems.backend.metadata.odoo.odoo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.odoo.Config;
import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.backend.metadata.odoo.odoo.jsonrpc.AuthenticateWithUsernameAndPasswordRequest;
import io.openems.backend.metadata.odoo.odoo.jsonrpc.AuthenticateWithUsernameAndPasswordResponse;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public class OdooHandler {

	protected final MetadataOdoo parent;

	private final Logger log = LoggerFactory.getLogger(OdooHandler.class);
	private final Credentials credentials;
	private final WriteWorker writeWorker;

	public OdooHandler(MetadataOdoo parent, Config config) {
		this.parent = parent;
		this.credentials = Credentials.fromConfig(config);
		this.writeWorker = new WriteWorker(this, this.credentials);
		this.writeWorker.start();
	}

	public WriteWorker getWriteWorker() {
		return writeWorker;
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
	 * @return the Odoo Session-ID
	 * @throws OpenemsNamedException on error
	 */
	public String authenticate(String username, String password) throws OpenemsNamedException {
		AuthenticateWithUsernameAndPasswordRequest request = new AuthenticateWithUsernameAndPasswordRequest(
				this.credentials.getDatabase(), username, password);
		JsonrpcResponseSuccess origResponse = OdooUtils
				.sendJsonrpcRequest(this.credentials.getUrl() + "/web/session/authenticate", request);
		AuthenticateWithUsernameAndPasswordResponse response = AuthenticateWithUsernameAndPasswordResponse
				.from(origResponse);
		return response.getSessionId();
	}

	/**
	 * Authenticates a user using a Session-ID.
	 * 
	 * @param sessionId the Odoo Session-ID
	 * @return the Odoo response
	 * @throws OpenemsNamedException on error
	 */
	public JsonrpcResponseSuccess authenticateSession(String sessionId) throws OpenemsNamedException {
		EmptyRequest request = new EmptyRequest();
		String charset = "US-ASCII";
		String query;
		try {
			query = String.format("session_id=%s", URLEncoder.encode(sessionId, charset));
		} catch (UnsupportedEncodingException e) {
			throw OpenemsError.GENERIC.exception(e.getMessage());
		}
		return OdooUtils.sendJsonrpcRequest(this.credentials.getUrl() + "/openems_backend/info?" + query, request);
	}

}
