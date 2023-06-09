package io.openems.backend.metadata.odoo.odoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(OdooUtils.class);

	private OdooUtils() {
	}

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
	 * Sends a JSON-RPC Request to an Odoo server - without Cookie header.
	 *
	 * @param url     the URL
	 * @param cookie  the Cookie
	 * @param request the JSON-RPC Request as {@link JsonObject}
	 * @return the {@link JsonObject} response and HTTP connection headers on
	 *         success
	 * @throws OpenemsNamedException on error
	 */
	public static SuccessResponseAndHeaders sendJsonrpcRequest(String url, String cookie, JsonObject request)
			throws OpenemsNamedException {
		return OdooUtils.sendJsonrpcRequest(url, cookie, request, 5000);
	}

	/**
	 * Sends a JSON-RPC Request to an Odoo server.
	 *
	 * @param url     the URL
	 * @param cookie  a Cookie string
	 * @param request the JSON-RPC Request as {@link JsonObject}
	 * @param timeout readtimeout in milliseconds
	 * @return the {@link JsonObject} response and HTTP connection headers on
	 *         success
	 * @throws OpenemsNamedException on error
	 */
	public static SuccessResponseAndHeaders sendJsonrpcRequest(String url, String cookie, JsonObject request,
			int timeout) throws OpenemsNamedException {
		HttpURLConnection connection = null;
		try {
			// Open connection to Odoo
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(5000);// 5 secs
			connection.setReadTimeout(timeout);// 5 secs
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
				var dataExceptionType = JsonUtils.getAsOptionalString(data, "exception_type");

				switch (dataName) {
				case "odoo.exceptions.AccessDenied", "odoo.http.SessionExpiredException" ->
					throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();

				default -> {
					var exception = "Exception for Request [" + request.toString() + "] to URL [" + url + "]: " //
							+ dataMessage + ";" //
							+ " Code [" + code + "]" //
							+ " Code [" + code + "]" //
							+ " Message [" + message + "]" //
							+ " Name [" + dataName + "]" //
							+ " ExceptionType [" + dataExceptionType.orElse("n/a") + "]" //
							+ " Arguments [" + dataArguments + "]" //
							+ " Debug [" + dataDebug + "]";
					throw new OpenemsException(exception);
				}
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

	protected static SuccessResponseAndHeaders sendAdminJsonrpcRequest(Credentials credentials, String url,
			JsonObject request, int timeout) throws OpenemsNamedException {
		var session = OdooUtils.login(credentials, "admin", credentials.getPassword());
		return OdooUtils.sendJsonrpcRequest(credentials.getUrl() + url, "session_id=" + session, request, timeout);
	}

	/**
	 * Sends a request with admin privileges.
	 *
	 * @param credentials the Odoo credentials
	 * @param url         to send the request
	 * @param request     to send
	 * @return SuccessResponseAndHeaders response
	 * @throws OpenemsNamedException on error
	 */
	protected static SuccessResponseAndHeaders sendAdminJsonrpcRequest(Credentials credentials, String url,
			JsonObject request) throws OpenemsNamedException {
		var session = OdooUtils.login(credentials, "admin", credentials.getPassword());
		return OdooUtils.sendJsonrpcRequest(credentials.getUrl() + url, "session_id=" + session, request);
	}

	/**
	 * Sends a request with admin privileges in async.
	 *
	 * @param credentials the Odoo credentials
	 * @param url         to send the request
	 * @param request     to send
	 * @return SuccessResponseAndHeaders response as Future
	 * @throws OpenemsNamedException on error
	 */
	protected static Future<SuccessResponseAndHeaders> sendAdminJsonrpcRequestAsync(Credentials credentials, String url,
			JsonObject request) throws OpenemsNamedException {
		var completableFuture = new CompletableFuture<SuccessResponseAndHeaders>();
		completableFuture.completeAsync(() -> {
			try {
				return sendAdminJsonrpcRequest(credentials, url, request);
			} catch (OpenemsNamedException e) {
				completableFuture.completeExceptionally(e);
			}
			return null;
		});
		return completableFuture;
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
		if (username.isBlank() || password.isBlank()) {
			// Do not even send request if username or password are blank
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		var request = JsonUtils.buildJsonObject() //
				.addProperty("jsonrpc", "2.0") //
				.addProperty("method", "call") //
				.add("params", JsonUtils.buildJsonObject() //
						.addProperty("db", credentials.getDatabase()) //
						.addProperty("login", username.toLowerCase()) //
						.addProperty("password", password) //
						.build()) //
				.build();
		var response = OdooUtils.sendJsonrpcRequest(credentials.getUrl() + "/web/session/authenticate", request);
		var sessionIdOpt = getFieldFromSetCookieHeader(response.headers, "session_id");
		if (!sessionIdOpt.isPresent()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		return sessionIdOpt.get();
	}

	private static Object executeKw(Credentials creds, String model, String action, Object[] arg)
			throws MalformedURLException, XMLRPCException {
		return executeKw(creds, model, action, arg, null);
	}

	private static Object executeKw(Credentials creds, String model, String action, Object[] arg, Map<String, ?> kw)
			throws MalformedURLException, XMLRPCException {
		var params = new Object[] { creds.getDatabase(), creds.getUid(), creds.getPassword(), model, action, arg, kw };
		var client = new XMLRPCClient(new URL(String.format("%s/xmlrpc/2/object", creds.getUrl())),
				XMLRPCClient.FLAGS_NIL);
		client.setTimeout(60 /* seconds */);
		return client.call("execute_kw", params);
	}

	protected static String[] getAsStringArray(Field... fields) {
		return Arrays.stream(fields) //
				.map(Field::id) //
				.toArray(String[]::new);
	}

	protected static Object[] getAsObjectArray(Domain... domains) {
		return Arrays.stream(domains) //
				.map(filter -> new Object[] { filter.field, filter.operator, filter.value }) //
				.toArray(Object[]::new);
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
	protected static Integer[] search(Credentials credentials, String model, Domain... domains)
			throws OpenemsException {
		// Add domain filter
		var domain = getAsObjectArray(domains);
		Object[] paramsDomain = { domain };
		try {
			// Execute XML request
			var resultObjs = (Object[]) OdooUtils.executeKw(credentials, model, "search", paramsDomain, Map.of());
			return Arrays.copyOf(resultObjs, resultObjs.length, Integer[].class);
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
	@SuppressWarnings("unchecked")
	protected static Map<String, Object> readOne(Credentials credentials, String model, int id, Field... fields)
			throws OpenemsException {
		// Create request params
		// Add ids
		var paramsIds = new Object[] { id };
		// Add fields
		var fieldStrings = getAsStringArray(fields);
		var paramsFields = Map.of("fields", fieldStrings);
		try {
			// Execute XML request
			var resultObjs = (Object[]) OdooUtils.executeKw(credentials, model, "read", paramsIds, paramsFields);
			// Parse results
			for (var resultObj : resultObjs) {
				return (Map<String, Object>) resultObj;
			}
			throw new OpenemsException("No matching entry found for id [" + id + "]");
		} catch (Throwable e) {
			throw new OpenemsException("Unable to read from Odoo: " + e.getMessage());
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
	@SuppressWarnings("unchecked")
	protected static Map<String, Object>[] readMany(Credentials credentials, String model, Integer[] ids,
			Field... fields) throws OpenemsException {
		var fieldStrings = getAsStringArray(fields);
		try {
			// Execute XML request
			var result = (Object[]) OdooUtils.executeKw(credentials, model, "read", new Object[] { ids, fieldStrings });
			return Arrays.copyOf(result, result.length, Map[].class);
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
	@SuppressWarnings("unchecked")
	protected static Map<String, Object>[] searchRead(Credentials credentials, String model, Field[] fields,
			Domain... domains) throws OpenemsException {
		// Create request params
		// Add domain filter
		var domain = getAsObjectArray(domains);
		var paramsDomain = new Object[] { domain };
		// Add fields
		var fieldStrings = getAsStringArray(fields);
		var paramsFields = Map.of("fields", fieldStrings);
		try {
			// Execute XML request
			var result = (Object[]) OdooUtils.executeKw(credentials, model, "search_read", paramsDomain, paramsFields);
			return Arrays.copyOf(result, result.length, Map[].class);
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
		try {
			// Execute XML request
			var resultObj = (Object[]) executeKw(credentials, "ir.model.data", "get_object_reference",
					new Object[] { module, name });
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
		try {
			// Execute XML request
			var resultObj = executeKw(credentials, model, "message_post", new Object[] { id, message });
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
		try {
			var resultObj = executeKw(credentials, model, "create", new Object[] { fieldValues });
			if (resultObj == null) {
				throw new OpenemsException("Not created.");
			}
			return OdooUtils.getAsOptional(resultObj, Integer.class).orElse(null);
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
		// Add fieldValues
		var paramsFieldValues = new HashMap<String, Object>();
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
		try {
			// Execute XML request
			var resultObj = (Boolean) OdooUtils.executeKw(credentials, model, "write",
					new Object[] { ids, fieldValues });
			if (!resultObj) {
				throw new OpenemsException("Returned False.");
			}
		} catch (Throwable e) {
			throw new OpenemsException("Unable to write to Odoo: " + e.getMessage());
		}
	}

	/**
	 * Return the Object type-safe as a {@link Optional} of type T; or otherwise as
	 * an empty {@link Optional}.
	 *
	 * @param <T>    expected type
	 * @param object the value as object
	 * @param type   the expected type of object
	 * @return the value as {@link Optional} String
	 */
	protected static <T extends Object> Optional<T> getAsOptional(Object object, Class<T> type) {
		if (type.isInstance(object)) {
			return Optional.of(type.cast(object));
		}
		return Optional.empty();
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

	/**
	 * Return Field value in values and cast it to type.
	 *
	 * @param <T>    expected type of value
	 * @param field  to search for
	 * @param values map with values to search in
	 * @param type   to cast into
	 * @return value found in map casted to type or null on error
	 */
	public static <T> T getAs(Field field, Map<String, ?> values, Class<T> type) {
		return getAsOrElse(field, values, type, null);
	}

	/**
	 * Return Field value in values and cast it to type.
	 *
	 * @param <T>       expected type of value
	 * @param field     to search for
	 * @param values    map with values to search in
	 * @param type      to cast into
	 * @param alternate value to return
	 * @return value found in map casted to type or alternate on error
	 */
	public static <T> T getAsOrElse(Field field, Map<String, ?> values, Class<T> type, T alternate) {
		if (field == null || values == null || type == null) {
			var warningMsg = new StringBuilder().append("[getAsOrElse] missing parameter (")
					.append(field == null ? "field is null, " : "").append(values == null ? "values is null, " : "")
					.append(type == null ? "type is null, " : "").toString();
			log.warn(warningMsg);
			return alternate;
		}

		if (!values.containsKey(field.id())) {
			return alternate;
		}

		var entry = values.get(field.id());
		var entryType = entry.getClass();

		// Entry equals false if table value is null;
		if (entryType.isAssignableFrom(Boolean.class) && !type.isAssignableFrom(Boolean.class)
				&& !Boolean.class.cast(entry)) {
			return null;
		}

		try {
			return type.cast(entry);
		} catch (Throwable t) {
			log.warn(t.getMessage());
			return alternate;
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
	private static Optional<String> getFieldFromSetCookieHeader(Map<String, List<String>> headers, String fieldname) {
		for (Entry<String, List<String>> header : headers.entrySet()) {
			var key = header.getKey();
			if (key != null && key.equalsIgnoreCase("Set-Cookie")) {
				for (String cookie : header.getValue()) {
					for (String cookieVariable : cookie.split("; ")) {
						var keyValue = cookieVariable.split("=");
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

	public static class DateTime {

		public static final ZoneId SERVER_TIMEZONE = ZoneId.of("UTC");
		public static final String SERVER_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
		public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(SERVER_DATETIME_FORMAT)
				.withZone(SERVER_TIMEZONE);

		/**
		 * Convert {@link String} in {@link OdooUtils.DEFAULT_SERVER_DATETIME_FORMAT}
		 * and UTC into {@link ZonedDateTime}.
		 *
		 * @param dateTimeString string in
		 *                       {@link OdooUtils.DEFAULT_SERVER_DATETIME_FORMAT} format
		 * @return ZonedDateTime representation, or null on error.
		 */
		public static ZonedDateTime stringToDateTime(String dateTimeString) {
			if (dateTimeString == null) {
				return null;
			}
			try {
				// Cut to format length
				var formatLength = SERVER_DATETIME_FORMAT.length();
				dateTimeString = dateTimeString.substring(0,
						formatLength > dateTimeString.length() ? dateTimeString.length() : formatLength);

				return ZonedDateTime.parse(dateTimeString, DATETIME_FORMATTER);
			} catch (DateTimeParseException e) {
				log.warn("'" + dateTimeString + "' is not of format " + SERVER_DATETIME_FORMAT, e);
				return null;
			}
		}

		/**
		 * Convert {@link ZonedDateTime} into {@link String} in
		 * {@link OdooUtils.DEFAULT_SERVER_DATETIME_FORMAT} and UTC.
		 *
		 * @param dateTime to parse
		 * @return String in {@link OdooUtils.DEFAULT_SERVER_DATETIME_FORMAT}
		 */
		public static String dateTimeToString(ZonedDateTime dateTime) {
			if (dateTime == null) {
				return null;
			}
			return dateTime.format(DATETIME_FORMATTER);
		}
	}
}
