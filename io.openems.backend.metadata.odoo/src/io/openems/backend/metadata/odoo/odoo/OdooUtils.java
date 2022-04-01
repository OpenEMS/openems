package io.openems.backend.metadata.odoo.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.timroes.axmlrpc.XMLRPCClient;
import de.timroes.axmlrpc.XMLRPCException;
import io.openems.backend.metadata.odoo.Field;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OdooUtils {

	private OdooUtils() {
	}

	public static final String DEFAULT_SERVER_DATE_FORMAT = "yyyy-MM-dd";
	public static final String DEFAULT_SERVER_TIME_FORMAT = "HH:mm:ss";
	public static final String DEFAULT_SERVER_DATETIME_FORMAT = OdooUtils.DEFAULT_SERVER_DATE_FORMAT + " "
			+ OdooUtils.DEFAULT_SERVER_TIME_FORMAT;

	public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter
			.ofPattern(OdooUtils.DEFAULT_SERVER_DATETIME_FORMAT);

	/**
	 * Wrapper for the reply of a call to
	 * {@link OdooUtils#sendJsonrpcRequest(String, JsonObject)}.
	 */
	public static class SuccessResponseAndHeaders {
		public final JsonElement result;
		public final Map<String, List<String>> headers;

		public SuccessResponseAndHeaders(JsonElement result, Map<String, List<String>> headers) {
			this.result = result;
			this.headers = headers;
		}
	}

	/**
	 * Sends a JSON-RPC Request to an Odoo server - without Cookie header.
	 *
	 * @param url     the URL
	 * @param request the JSON-RPC Request as {@link JsonObject}
	 * @return the {@link JsonObject} response and HTTP connection headers on
	 *         success
	 * @throws OpenemsNamedException on error
	 */
	public static SuccessResponseAndHeaders sendJsonrpcRequest(String url, JsonObject request)
			throws OpenemsNamedException {
		return OdooUtils.sendJsonrpcRequest(url, "", request);
	}

	/**
	 * Sends a JSON-RPC Request to an Odoo server.
	 *
	 * @param url     the URL
	 * @param cookie  a Cookie string
	 * @param request the JSON-RPC Request as {@link JsonObject}
	 * @return the {@link JsonObject} response and HTTP connection headers on
	 *         success
	 * @throws OpenemsNamedException on error
	 */
	public static SuccessResponseAndHeaders sendJsonrpcRequest(String url, String cookie, JsonObject request)
			throws OpenemsNamedException {
		HttpURLConnection connection = null;
		try {
			// Open connection to Odoo
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(5000);// 5 secs
			connection.setRequestProperty("Accept-Charset", "US-ASCII");
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			if (!cookie.isEmpty()) {
				connection.setRequestProperty("Cookie", cookie);
			}

			// send JSON-RPC request
			try (var out = new OutputStreamWriter(connection.getOutputStream())) {
				out.write(request.toString());
				out.flush();
			}

			// read JSON-RPC response
			var sb = new StringBuilder();
			String line = null;
			try (var br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
			var json = JsonUtils.parseToJsonObject(sb.toString());

			// Handle Success or Error
			if (json.has("error")) {
				var error = JsonUtils.getAsJsonObject(json, "error");
				// "code":200",
				var code = JsonUtils.getAsInt(error, "code");
				// "message":"Odoo Server Error",
				var message = JsonUtils.getAsString(error, "message");
				var data = JsonUtils.getAsJsonObject(error, "data");
				// "name":"odoo.exceptions.AccessDenied",
				var dataName = JsonUtils.getAsString(data, "name");
				// "debug":"Traceback (most recent call last):\n...",
				var dataDebug = JsonUtils.getAsString(data, "debug");
				// "message":"Access denied",
				var dataMessage = JsonUtils.getAsString(data, "message");
				// "arguments":["Access denied"],
				var dataArguments = JsonUtils.getAsJsonArray(data, "arguments");
				// "exception_type":"access_denied"
				var dataExceptionType = JsonUtils.getAsString(data, "exception_type");
				switch (dataName) {
				case "odoo.exceptions.AccessDenied":
					throw new OpenemsException(
							"Access Denied for Request [" + request.toString() + "] to URL [" + url + "]");
				case "odoo.http.SessionExpiredException":
					throw new OpenemsException("Session Expired for Request to URL [" + url + "]");
				default:
					var exception = "Exception for Request [" + request.toString() + "] to URL [" + url + "]: " //
							+ dataMessage + ";" //
							+ " Code [" + code + "]" //
							+ " Code [" + code + "]" //
							+ " Message [" + message + "]" //
							+ " Name [" + dataName + "]" //
							+ " ExceptionType [" + dataExceptionType + "]" //
							+ " Arguments [" + dataArguments + "]" //
							+ " Debug [" + dataDebug + "]";
					throw new OpenemsException(exception);
				}
			} else if (json.has("result")) {
				return new SuccessResponseAndHeaders(JsonUtils.getSubElement(json, "result"),
						connection.getHeaderFields());

			} else {
				// JSON-RPC response by Odoo on /logout is {jsonrpc:2.0, id:null} - without
				// 'result' attribute
				return new SuccessResponseAndHeaders(json, connection.getHeaderFields());

			}

		} catch (IOException e) {
			throw OpenemsError.GENERIC.exception(e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Sends a request with admin privileges.
	 *
	 * @param credentials the Odoo credentials
	 * @param url         to send the request
	 * @param request     to send
	 * @throws OpenemsNamedException on error
	 */
	protected static void sendAdminJsonrpcRequest(Credentials credentials, String url, JsonObject request)
			throws OpenemsNamedException {
		var session = OdooUtils.login(credentials, "admin", credentials.getPassword());
		OdooUtils.sendJsonrpcRequest(credentials.getUrl() + url, "session_id=" + session, request);
	}

	/**
	 * Authenticates a user using Username and Password.
	 *
	 * @param credentials used to get Odoo url
	 * @param username    the Username
	 * @param password    the Password
	 * @return the session_id
	 * @throws OpenemsNamedException on login error
	 */
	protected static String login(Credentials credentials, String username, String password)
			throws OpenemsNamedException {
		var request = JsonUtils.buildJsonObject() //
				.addProperty("jsonrpc", "2.0") //
				.addProperty("method", "call") //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("db", credentials.getDatabase()) //
						.addProperty("login", username.toLowerCase()) //
						.addProperty("password", password) //
						.build()) //
				.build();
		SuccessResponseAndHeaders response = OdooUtils
				.sendJsonrpcRequest(credentials.getUrl() + "/web/session/authenticate", request);
		var sessionIdOpt = OdooHandler.getFieldFromSetCookieHeader(response.headers, "session_id");
		if (!sessionIdOpt.isPresent()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		return sessionIdOpt.get();
	}

	private static Object executeKw(String url, Object[] params) throws MalformedURLException, XMLRPCException {
		var client = new XMLRPCClient(new URL(String.format("%s/xmlrpc/2/object", url)), XMLRPCClient.FLAGS_NIL);
		client.setTimeout(60 /* seconds */);
		return client.call("execute_kw", params);
	}

	/**
	 * Executes a search on Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param domains     Odoo domain filters
	 * @return Odoo object ids
	 * @throws OpenemsException on error
	 */
	protected static int[] search(Credentials credentials, String model, Domain... domains) throws OpenemsException {
		// Add domain filter
		Object[] domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			Domain filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = { domain };
		// Create request params
		HashMap<Object, Object> paramsRules = new HashMap<Object, Object>();
		String action = "search";
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model, action,
				paramsDomain, paramsRules };
		try {
			// Execute XML request
			var resultObjs = (Object[]) OdooUtils.executeKw(credentials.getUrl(), params);
			// Parse results
			int[] results = new int[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				results[i] = (int) resultObjs[i];
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Reads a record from Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param id          id of model to read
	 * @param fields      fields that should be read
	 * @return the record as a Map
	 * @throws OpenemsException on error
	 */
	protected static Map<String, Object> readOne(Credentials credentials, String model, int id, Field... fields)
			throws OpenemsException {
		// Create request params
		// Add ids
		var paramsIds = new Object[1];
		paramsIds[0] = id;
		// Add fields
		var fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].id();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model, "read",
				paramsIds, paramsFields };
		try {
			// Execute XML request
			var resultObjs = (Object[]) OdooUtils.executeKw(credentials.getUrl(), params);
			// Parse results
			for (var resultObj : resultObjs) {
				@SuppressWarnings("unchecked")
				var result = (Map<String, Object>) resultObj;
				return result;
			}
			throw new OpenemsException("No matching entry found for id [" + id + "]");
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Executes a Search and read on Odoo.
	 *
	 * @see <a href=
	 *      "https://www.odoo.com/documentation/10.0/api_integration.html">Odoo API
	 *      Integration</a>
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param domains     Odoo domain filters
	 * @param fields      the Fields
	 * @return Odoo object ids
	 * @throws OpenemsException on error
	 */
	// TODO this method is not yet functional
	protected static Map<String, Object>[] searchAndRead(Credentials credentials, String model, Domain[] domains,
			Field[] fields) throws OpenemsException {
		// Add domain filter
		var domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			Domain filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = { domain };
		// Add fields
		var fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].id();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		var action = "search_read";
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model, action,
				paramsDomain, paramsFields };
		try {
			// Execute XML request
			OdooUtils.executeKw(credentials.getUrl(), params);
			// Object[] resultObjs = (Object[]) executeKw(url, params);
			// Parse results
			// int[] results = new int[resultObjs.length];
			// for (int i = 0; i < resultObjs.length; i++) {
			// results[i] = (int) resultObjs[i];
			// }
			return null;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to search and read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Reads multiple records from Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param ids         ids of model to read
	 * @param fields      fields that should be read
	 * @return the records as a Map array
	 * @throws OpenemsException on error
	 */
	protected static Map<String, Object>[] readMany(Credentials credentials, String model, Integer[] ids,
			Field... fields) throws OpenemsException {
		// Create request params
		var action = "read";
		// Add ids
		// Object[] paramsIds = Arrays.stream(ids).mapToObj(id -> (Integer)
		// id).toArray();
		// Object[] paramsIds = new Object[2];
		// paramsIds[0] = ids[0];
		// paramsIds[1] = ids[1];
		// Add fields
		var fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].id();
		}
		// Map<String, String[]> paramsFields = new HashMap<>();
		// paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model, action,
				new Object[] { ids, fieldStrings } };
		try {
			// Execute XML request
			var resultObjs = (Object[]) OdooUtils.executeKw(credentials.getUrl(), params);
			// Parse results
			@SuppressWarnings("unchecked")
			Map<String, Object>[] results = new Map[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				@SuppressWarnings("unchecked")
				var result = (Map<String, Object>) resultObjs[i];
				results[i] = result;
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Search-Reads multiple records from Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model to query (e.g. 'res.partner')
	 * @param fields      fields that should be read
	 * @param domains     filter domains
	 * @return the records as a Map array
	 * @throws OpenemsException on error
	 */
	protected static Map<String, Object>[] searchRead(Credentials credentials, String model, Field[] fields,
			Domain... domains) throws OpenemsException {
		// Create request params
		// Add domain filter
		var domain = new Object[domains.length];
		for (int i = 0; i < domains.length; i++) {
			var filter = domains[i];
			domain[i] = new Object[] { filter.field, filter.operator, filter.value };
		}
		Object[] paramsDomain = { domain };
		// Add fields
		var fieldStrings = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			fieldStrings[i] = fields[i].toString();
		}
		Map<String, String[]> paramsFields = new HashMap<>();
		paramsFields.put("fields", fieldStrings);
		// Create request params
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model,
				"search_read", paramsDomain, paramsFields };
		try {
			// Execute XML request
			var resultObjs = (Object[]) OdooUtils.executeKw(credentials.getUrl(), params);
			// Parse results
			@SuppressWarnings("unchecked")
			Map<String, Object>[] results = new Map[resultObjs.length];
			for (int i = 0; i < resultObjs.length; i++) {
				@SuppressWarnings("unchecked")
				var result = (Map<String, Object>) resultObjs[i];
				results[i] = result;
			}
			return results;
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Executes a get object reference from Odoo.
	 * 
	 * @param credentials the Odoo credentials
	 * @param module      the Odoo module
	 * @param name        the external identifier
	 * @return internal id of external identifier
	 * @throws OpenemsException on error
	 */
	protected static int getObjectReference(Credentials credentials, String module, String name)
			throws OpenemsException {
		// Create request params
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), "ir.model.data",
				"get_object_reference", new Object[] { module, name } };
		try {
			// Execute XML request
			var resultObj = (Object[]) executeKw(credentials.getUrl(), params);
			if (resultObj == null) {
				throw new OpenemsException(
						"No matching entry found for module [" + module + "] and name [" + name + "]");
			}

			return (int) resultObj[1];
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
		}
	}

	/**
	 * Adds a message in Odoo Chatter ('mail.thread').
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model (e.g. 'res.partner')
	 * @param id          id of model
	 * @param message     the message
	 * @throws OpenemsException on error
	 */
	protected static void addChatterMessage(Credentials credentials, String model, int id, String message)
			throws OpenemsException {
		// Create request params
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model,
				"message_post", new Object[] { id, message } };
		try {
			// Execute XML request
			Object resultObj = executeKw(credentials.getUrl(), params);
			if (resultObj == null) {
				throw new OpenemsException("Returned Null");
			}
		} catch (Throwable e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		}
	}

	/**
	 * Create a record in Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       the Oddo model
	 * @param fieldValues fields and values that should be written
	 * @return Odoo id of created record
	 * @throws OpenemsException on error
	 */
	protected static int create(Credentials credentials, String model, FieldValue<?>... fieldValues)
			throws OpenemsException {
		Map<String, Object> paramsFieldValues = new HashMap<>();
		for (FieldValue<?> fieldValue : fieldValues) {
			paramsFieldValues.put(fieldValue.getField().id(), fieldValue.getValue());
		}

		return OdooUtils.create(credentials, model, paramsFieldValues);
	}

	/**
	 * Create a record in Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       the Oddo model
	 * @param fieldValues fields and values that should be written
	 * @return Odoo id of created record
	 * @throws OpenemsException on error
	 */
	protected static int create(Credentials credentials, String model, Map<String, Object> fieldValues)
			throws OpenemsException {
		var action = "create";

		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model, action,
				new Object[] { fieldValues } };

		try {
			Object resultObj = (Object) executeKw(credentials.getUrl(), params);
			if (resultObj == null) {
				throw new OpenemsException("Not created.");
			}

			return OdooUtils.getAsInteger(resultObj);
		} catch (Throwable e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		}
	}

	/**
	 * Update a record in Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       the Odoo model
	 * @param ids         ids of model to update
	 * @param fieldValues fields and values that should be written
	 * @throws OpenemsException on error
	 */
	public static void write(Credentials credentials, String model, Integer[] ids, FieldValue<?>... fieldValues)
			throws OpenemsException {
		// // for debugging:
		// StringBuilder b = new StringBuilder("Odoo Write: " + model + "; ");
		// for (int id : ids) {
		// b.append(id + ",");
		// }
		// b.append(";");
		// for (FieldValue fieldValue : fieldValues) {
		// b.append(fieldValue.getField().n() + ",");
		// }
		// System.out.println(b.toString());

		// Add fieldValues
		Map<String, Object> paramsFieldValues = new HashMap<>();
		for (FieldValue<?> fieldValue : fieldValues) {
			paramsFieldValues.put(fieldValue.getField().id(), fieldValue.getValue());
		}

		OdooUtils.write(credentials, model, ids, paramsFieldValues);
	}

	/**
	 * Update a record in Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       the Odoo model
	 * @param ids         ids of model to update
	 * @param fieldValues fields and values that should be written
	 * @throws OpenemsException on error
	 */
	protected static void write(Credentials credentials, String model, Integer[] ids, Map<String, Object> fieldValues)
			throws OpenemsException {
		// Create request params
		var action = "write";

		// Create request params
		Object[] params = { credentials.getDatabase(), credentials.getUid(), credentials.getPassword(), model, action,
				new Object[] { ids, fieldValues } };
		try {
			// Execute XML request
			var resultObj = (Boolean) OdooUtils.executeKw(credentials.getUrl(), params);
			if (!resultObj) {
				throw new OpenemsException("Returned False.");
			}
		} catch (Throwable e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		}
	}

	/**
	 * Return the Object type-safe as a String; or otherwise as an empty String.
	 *
	 * @param object the value as object
	 * @return the value as String
	 */
	protected static String getAsString(Object object) {
		if (object instanceof String) {
			return (String) object;
		}
		return "";
	}

	/**
	 * Return the Object type-safe as a Integer; or otherwise null.
	 *
	 * @param object the value as object
	 * @return the value as Integer
	 */
	protected static Integer getAsInteger(Object object) {
		if (object instanceof Integer) {
			return (Integer) object;
		}
		return null;
	}

	/**
	 * Return the odoo reference id as a {@link Integer}, otherwise empty
	 * {@link Optional}.
	 *
	 * @param object the odoo reference to extract
	 * @return the odoo reference id or empty {@link Optional}
	 */
	protected static Optional<Integer> getOdooReferenceId(Object object) {
		if (object instanceof Object[]) {
			var odooReference = (Object[]) object;

			if (odooReference.length > 0 && odooReference[0] instanceof Integer) {
				return Optional.of((Integer) odooReference[0]);
			}
		}

		return Optional.empty();
	}

	/**
	 * Returns a Odoo report as a byte array. Search for the given template id in
	 * combination with the concrete report id.
	 *
	 * @param credentials the Odoo credentialss
	 * @param report      the Odoo template id
	 * @param id          the Odoo report id
	 * @return the Odoo report as a byte array
	 * @throws OpenemsNamedException on error
	 */
	protected static byte[] getOdooReport(Credentials credentials, String report, int id) throws OpenemsNamedException {
		var session = OdooUtils.login(credentials, "admin", credentials.getPassword());

		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(
					credentials.getUrl() + "/report/pdf/" + report + "/" + id + "?session_id=" + session)
							.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);

			return ByteStreams.toByteArray(connection.getInputStream());
		} catch (Exception e) {
			throw OpenemsError.GENERIC.exception(e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
