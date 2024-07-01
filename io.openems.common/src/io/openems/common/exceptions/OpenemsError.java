package io.openems.common.exceptions;

import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

/**
 * Holds named OpenEMS Errors.
 */
public enum OpenemsError {
	/*
	 * Generic error.
	 */
	GENERIC(1, "%s"),
	/*
	 * Common errors. 1000-1999
	 */
	COMMON_NO_VALID_CHANNEL_ADDRESS(1000, "This [%s] is not a valid channel address"), //
	COMMON_USER_NOT_AUTHENTICATED(1001, "User is not authenticated. [%s]"), //
	COMMON_ROLE_ACCESS_DENIED(1002, "Access to this resource [%s] is denied for User with Role [%s]"), //
	COMMON_AUTHENTICATION_FAILED(1003, "Authentication failed"), //
	COMMON_USER_UNDEFINED(1004, "User [%s] is not defined"), //
	COMMON_ROLE_UNDEFINED(1005, "Access to this resource [%s] is denied. Role for User [%s] is not defined"), //
	/*
	 * Edge errors. 2000-2999
	 */
	EDGE_NO_COMPONENT_WITH_ID(2000, "Unable to find OpenEMS Component with ID [%s]"), //
	EDGE_MULTIPLE_COMPONENTS_WITH_ID(2001, "Found more than one OpenEMS Component with ID [%s]"), //
	EDGE_UNABLE_TO_APPLY_CONFIG(2002, "Unable to apply configuration to Component [%s]: [%s]"), //
	EDGE_UNABLE_TO_CREATE_CONFIG(2003, "Unable to create configuration for Factory [%s]: [%s]"), //
	EDGE_UNABLE_TO_DELETE_CONFIG(2004, "Unable to delete configuration for Component [%s]: [%s]"), //
	EDGE_CHANNEL_NO_OPTION(2005, "Channel has no Option [%s]. Existing options: %s"), //
	/*
	 * Backend errors. 3000-3999
	 */
	BACKEND_EDGE_NOT_CONNECTED(3000, "Edge [%s] is not connected"), //
	BACKEND_UI_TOKEN_MISSING(3001, "Token for UI connection is missing"), //
	BACKEND_NO_UI_WITH_TOKEN(3002, "No open connection with Token [%s]"), //
	/*
	 * JSON-RPC Request/Response/Notification. 4000-4999
	 */
	JSONRPC_ID_NOT_UNIQUE(4000, "A Request with this ID [%s] had already been existing"), //
	JSONRPC_UNHANDLED_METHOD(4001, "Unhandled JSON-RPC method [%s]"), //
	JSONRPC_INVALID_MESSAGE(4002, "JSON-RPC Message is not a valid Request, Result or Notification: %s"), //
	JSONRPC_RESPONSE_WITHOUT_REQUEST(4003, "Got Response without Request: %s"), //

	/*
	 * JSON Errors. 5000-5999
	 */
	JSON_HAS_NO_MEMBER(5000, "JSON [%s] is not a member of [%s]"), //
	JSON_NO_INTEGER(5019, "JSON [%s] is not an Integer"), //
	JSON_NO_INTEGER_MEMBER(5001, "JSON [%s:%s] is not an Integer"), //
	JSON_NO_OBJECT(5002, "JSON [%s] is not a JSON-Object"), //
	JSON_NO_OBJECT_MEMBER(5003, "JSON [%s] ist not a member of JSON-Object [%s]"), //
	JSON_NO_PRIMITIVE(5004, "JSON [%s] is not a JSON-Primitive"), //
	JSON_NO_PRIMITIVE_MEMBER(5005, "JSON [%s] is not a JSON-Primitive member of JSON-Object [%s]"), //
	JSON_NO_ARRAY(5006, "JSON [%s] is not JSON-Array"), //
	JSON_NO_ARRAY_MEMBER(5007, "JSON [%s:%s] is not JSON-Array"), //
	JSON_NO_DATE_MEMBER(5008, "JONS [%s:%s] is not a Date. Error: %s"), //
	JSON_NO_STRING(5009, "JSON [%s] is not a String"), //
	JSON_NO_STRING_MEMBER(5010, "JSON [%s:%s] is not a String member"), //
	JSON_NO_BOOLEAN(5011, "JSON [%s] is not a Boolean"), //
	JSON_NO_BOOLEAN_MEMBER(5012, "JSON [%s:%s] is not a Boolean member"), //
	JSON_NO_NUMBER(5013, "JSON [%s] is not a Number"), //
	JSON_NO_NUMBER_MEMBER(5014, "JSON [%s:%s] is not a Number member"), //
	JSON_PARSE_ELEMENT_FAILED(5015, "JSON failed to parse [%s]. %s: %s"), //
	JSON_PARSE_FAILED(5016, "JSON failed to parse [%s]: %s"), //
	JSON_NO_ENUM_MEMBER(5018, "JSON [%s:%s] is not an Enum member"), //
	JSON_NO_INET4ADDRESS(5020, "JSON [%s] is not an IPv4 address"), //
	JSON_NO_ENUM(5021, "JSON [%s] is not an Enum"), //
	JSON_NO_FLOAT(5022, "JSON [%s] is not a Float"), //
	JSON_NO_FLOAT_MEMBER(5030, "JSON [%s:%s] is not a Float member"), //
	JSON_NO_SHORT(5023, "JSON [%s] is not a Short"), //
	JSON_NO_SHORT_MEMBER(5024, "JSON [%s:%s] is not a Short member"), //
	JSON_NO_LONG(5025, "JSON [%s] is not a Short"), //
	JSON_NO_LONG_MEMBER(5026, "JSON [%s:%s] is not a Short member"), //
	JSON_NO_DOUBLE(5027, "JSON [%s] is not a Short"), //
	JSON_NO_DOUBLE_MEMBER(5028, "JSON [%s:%s] is not a Short member"), //
	JSON_NO_STRING_ARRAY(5029, "JSON [%s] is not a String Array"), //
	JSON_NO_INET4ADDRESS_MEMBER(5031, "JSON [%s:%s] is not a IPv4 address member"), //
	JSON_NO_UUID(5032, "JSON [%s] is not a UUID"), //
	JSON_NO_UUID_MEMBER(5033, "JSON [%s:%s] is not a UUID member"), //
	/*
	 * XML Errors. 6000-6999
	 */
	XML_HAS_NO_MEMBER(6000, "XML [%s] has no member [%s]"), //
	XML_NO_STRING_MEMBER(6010, "XML [%s:%s] is not a String member"), //
	;

	/**
	 * Gets an OpenEMS-Error from its code.
	 *
	 * @param code the error code
	 * @return the OpenEMS-Error
	 * @throws OpenemsException if no standard exception with this error code
	 *                          exists.
	 */
	public static OpenemsError fromCode(int code) throws OpenemsException {
		var error = OpenemsError.ALL_ERRORS.get(code);
		if (error == null) {
			throw new OpenemsException("OpenEMS-Error with code [" + code + "] does not exist");
		}
		return error;
	}

	private static final Logger log = LoggerFactory.getLogger(OpenemsError.class);

	private static final TreeMap<Integer, OpenemsError> ALL_ERRORS = new TreeMap<>();

	private final int code;
	private final String message;
	private final int noOfParams;

	private OpenemsError(int code, String message) {
		this.code = code;
		this.message = message;
		this.noOfParams = CharMatcher.is('%').countIn(message);
	}

	public int getCode() {
		return this.code;
	}

	public String getRawMessage() {
		return this.message;
	}

	/**
	 * Gets the formatted Error message.
	 *
	 * @param params the error parameters
	 * @return the error message as String
	 */
	public String getMessage(Object... params) {
		if (params.length != this.noOfParams) {
			OpenemsError.log.warn("OpenEMS-Error [" + this.name() + "] expects [" + this.noOfParams + "] params, got ["
					+ params.length + "]");
		}
		return String.format(this.message, params);
	}

	/*
	 * Fill ALL_ERRORS map and check for duplicate Error codes.
	 */
	static {
		for (OpenemsError error : OpenemsError.values()) {
			var duplicate = OpenemsError.ALL_ERRORS.putIfAbsent(error.code, error);
			if (duplicate != null) {
				OpenemsError.log.warn("Duplicate OpenEMS-Error with code [" + error.code + "]");
			}
		}
	}

	/**
	 * Creates a OpenEMS Named Exception from this Error.
	 * 
	 * <p>
	 * Use like: `throw OpenemsError.GENERIC.exception(...)`
	 *
	 * @param params the params for the Error message
	 * @return OpenemsNamedException
	 */
	public OpenemsNamedException exception(Object... params) {
		return new OpenemsNamedException(this, params);
	}

	public static class OpenemsNamedException extends Exception {

		private static final long serialVersionUID = 1L;

		private final OpenemsError error;
		private final Object[] params;

		public OpenemsNamedException(OpenemsError error, Object... params) {
			super(error.getMessage(params));
			this.error = error;
			this.params = params;
		}

		public OpenemsError getError() {
			return this.error;
		}

		public int getCode() {
			return this.error.getCode();
		}

		public Object[] getParams() {
			return this.params;
		}
	}
}
