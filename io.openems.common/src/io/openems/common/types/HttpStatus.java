package io.openems.common.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record HttpStatus(int code, String description) {
	// 1xx Informational
	public static final HttpStatus CONTINUE = new HttpStatus(100, "Continue");
	public static final HttpStatus SWITCHING_PROTOCOLS = new HttpStatus(101, "Switching Protocols");
	public static final HttpStatus PROCESSING = new HttpStatus(102, "Processing");
	public static final HttpStatus EARLY_HINTS = new HttpStatus(103, "Early Hints");

	// 2xx Success
	public static final HttpStatus OK = new HttpStatus(200, "OK");
	public static final HttpStatus CREATED = new HttpStatus(201, "Created");
	public static final HttpStatus ACCEPTED = new HttpStatus(202, "Accepted");
	public static final HttpStatus NON_AUTHORITATIVE_INFORMATION = new HttpStatus(203, "Non-Authoritative Information");
	public static final HttpStatus NO_CONTENT = new HttpStatus(204, "No Content");
	public static final HttpStatus RESET_CONTENT = new HttpStatus(205, "Reset Content");
	public static final HttpStatus PARTIAL_CONTENT = new HttpStatus(206, "Partial Content");
	public static final HttpStatus MULTI_STATUS = new HttpStatus(207, "Multi-Status");
	public static final HttpStatus ALREADY_REPORTED = new HttpStatus(208, "Already Reported");
	public static final HttpStatus IM_USED = new HttpStatus(226, "IM Used");

	// 3xx Redirection
	public static final HttpStatus MULTIPLE_CHOICES = new HttpStatus(300, "Multiple Choices");
	public static final HttpStatus MOVED_PERMANENTLY = new HttpStatus(301, "Moved Permanently");
	public static final HttpStatus FOUND = new HttpStatus(302, "Found");
	public static final HttpStatus SEE_OTHER = new HttpStatus(303, "See Other");
	public static final HttpStatus NOT_MODIFIED = new HttpStatus(304, "Not Modified");
	public static final HttpStatus USE_PROXY = new HttpStatus(305, "Use Proxy");
	public static final HttpStatus TEMPORARY_REDIRECT = new HttpStatus(307, "Temporary Redirect");
	public static final HttpStatus PERMANENT_REDIRECT = new HttpStatus(308, "Permanent Redirect");

	// 4xx Client Error
	public static final HttpStatus BAD_REQUEST = new HttpStatus(400, "Bad Request");
	public static final HttpStatus UNAUTHORIZED = new HttpStatus(401, "Unauthorized");
	public static final HttpStatus PAYMENT_REQUIRED = new HttpStatus(402, "Payment Required");
	public static final HttpStatus FORBIDDEN = new HttpStatus(403, "Forbidden");
	public static final HttpStatus NOT_FOUND = new HttpStatus(404, "Not Found");
	public static final HttpStatus METHOD_NOT_ALLOWED = new HttpStatus(405, "Method Not Allowed");
	public static final HttpStatus NOT_ACCEPTABLE = new HttpStatus(406, "Not Acceptable");
	public static final HttpStatus PROXY_AUTHENTICATION_REQUIRED = new HttpStatus(407, "Proxy Authentication Required");
	public static final HttpStatus REQUEST_TIMEOUT = new HttpStatus(408, "Request Timeout");
	public static final HttpStatus CONFLICT = new HttpStatus(409, "Conflict");
	public static final HttpStatus GONE = new HttpStatus(410, "Gone");
	public static final HttpStatus LENGTH_REQUIRED = new HttpStatus(411, "Length Required");
	public static final HttpStatus PRECONDITION_FAILED = new HttpStatus(412, "Precondition Failed");
	public static final HttpStatus PAYLOAD_TOO_LARGE = new HttpStatus(413, "Payload Too Large");
	public static final HttpStatus URI_TOO_LONG = new HttpStatus(414, "URI Too Long");
	public static final HttpStatus UNSUPPORTED_MEDIA_TYPE = new HttpStatus(415, "Unsupported Media Type");
	public static final HttpStatus RANGE_NOT_SATISFIABLE = new HttpStatus(416, "Range Not Satisfiable");
	public static final HttpStatus EXPECTATION_FAILED = new HttpStatus(417, "Expectation Failed");
	public static final HttpStatus IM_A_TEAPOT = new HttpStatus(418, "I'm a teapot");
	public static final HttpStatus MISDIRECTED_REQUEST = new HttpStatus(421, "Misdirected Request");
	public static final HttpStatus UNPROCESSABLE_ENTITY = new HttpStatus(422, "Unprocessable Entity");
	public static final HttpStatus LOCKED = new HttpStatus(423, "Locked");
	public static final HttpStatus FAILED_DEPENDENCY = new HttpStatus(424, "Failed Dependency");
	public static final HttpStatus TOO_EARLY = new HttpStatus(425, "Too Early");
	public static final HttpStatus UPGRADE_REQUIRED = new HttpStatus(426, "Upgrade Required");
	public static final HttpStatus PRECONDITION_REQUIRED = new HttpStatus(428, "Precondition Required");
	public static final HttpStatus TOO_MANY_REQUESTS = new HttpStatus(429, "Too Many Requests");
	public static final HttpStatus REQUEST_HEADER_FIELDS_TOO_LARGE = new HttpStatus(431,
			"Request Header Fields Too Large");
	public static final HttpStatus UNAVAILABLE_FOR_LEGAL_REASONS = new HttpStatus(451, "Unavailable For Legal Reasons");

	// 5xx Server Error
	public static final HttpStatus INTERNAL_SERVER_ERROR = new HttpStatus(500, "Internal Server Error");
	public static final HttpStatus NOT_IMPLEMENTED = new HttpStatus(501, "Not Implemented");
	public static final HttpStatus BAD_GATEWAY = new HttpStatus(502, "Bad Gateway");
	public static final HttpStatus SERVICE_UNAVAILABLE = new HttpStatus(503, "Service Unavailable");
	public static final HttpStatus GATEWAY_TIMEOUT = new HttpStatus(504, "Gateway Timeout");
	public static final HttpStatus HTTP_VERSION_NOT_SUPPORTED = new HttpStatus(505, "HTTP Version Not Supported");
	public static final HttpStatus VARIANT_ALSO_NEGOTIATES = new HttpStatus(506, "Variant Also Negotiates");
	public static final HttpStatus INSUFFICIENT_STORAGE = new HttpStatus(507, "Insufficient Storage");
	public static final HttpStatus LOOP_DETECTED = new HttpStatus(508, "Loop Detected");
	public static final HttpStatus NOT_EXTENDED = new HttpStatus(510, "Not Extended");
	public static final HttpStatus NETWORK_AUTHENTICATION_REQUIRED = new HttpStatus(511,
			"Network Authentication Required");

	private static final Map<Integer, HttpStatus> CODE_MAP;

	static {
		CODE_MAP = new HashMap<>(62);
		for (final var statusCode : List.of(CONTINUE, SWITCHING_PROTOCOLS, PROCESSING, EARLY_HINTS, OK, CREATED,
				ACCEPTED, NON_AUTHORITATIVE_INFORMATION, NO_CONTENT, RESET_CONTENT, PARTIAL_CONTENT, MULTI_STATUS,
				ALREADY_REPORTED, IM_USED, MULTIPLE_CHOICES, MOVED_PERMANENTLY, FOUND, SEE_OTHER, NOT_MODIFIED,
				USE_PROXY, TEMPORARY_REDIRECT, PERMANENT_REDIRECT, BAD_REQUEST, UNAUTHORIZED, PAYMENT_REQUIRED,
				FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, NOT_ACCEPTABLE, PROXY_AUTHENTICATION_REQUIRED,
				REQUEST_TIMEOUT, CONFLICT, GONE, LENGTH_REQUIRED, PRECONDITION_FAILED, PAYLOAD_TOO_LARGE, URI_TOO_LONG,
				UNSUPPORTED_MEDIA_TYPE, RANGE_NOT_SATISFIABLE, EXPECTATION_FAILED, IM_A_TEAPOT, MISDIRECTED_REQUEST,
				UNPROCESSABLE_ENTITY, LOCKED, FAILED_DEPENDENCY, TOO_EARLY, UPGRADE_REQUIRED, PRECONDITION_REQUIRED,
				TOO_MANY_REQUESTS, REQUEST_HEADER_FIELDS_TOO_LARGE, UNAVAILABLE_FOR_LEGAL_REASONS,
				INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT,
				HTTP_VERSION_NOT_SUPPORTED, VARIANT_ALSO_NEGOTIATES, INSUFFICIENT_STORAGE, LOOP_DETECTED, NOT_EXTENDED,
				NETWORK_AUTHENTICATION_REQUIRED)) {
			CODE_MAP.put(statusCode.code(), statusCode);
		}
	}

	/**
	 * Gets the HttpStatus from the predefined status codes with the
	 * {@link HttpStatus#code} matching the provided code.
	 * 
	 * @param code the code to find the corresponding {@link HttpStatus}
	 * @return if found {@link HttpStatus}; else null
	 */
	public static HttpStatus fromCodeOrNull(int code) {
		return CODE_MAP.get(code);
	}

	/**
	 * Gets the HttpStatus from the predefined status codes with the
	 * {@link HttpStatus#code} matching the provided code. If there is no predefined
	 * {@link HttpStatus} a new custom {@link HttpStatus} is created.
	 * 
	 * @param code the code to find the corresponding {@link HttpStatus}
	 * @return if found {@link HttpStatus}; else a custom {@link HttpStatus} from
	 *         the provided arguments
	 */
	public static HttpStatus fromCodeOrCustom(int code, String description) {
		final var predefinedStatus = HttpStatus.fromCodeOrNull(code);
		if (predefinedStatus != null) {
			return predefinedStatus;
		}
		return new HttpStatus(code, description);
	}

	/**
	 * Checks if the HTTP status code is defined as a information response
	 * (100-199).
	 * 
	 * @param status the HTTP status to check
	 * @return true if the status is defined as informational; else false
	 */
	public static final boolean isStatusInformational(int status) {
		return status >= 100 && status < 200;
	}

	/**
	 * Checks if the HTTP status code is defined as a successful response (200-299).
	 * 
	 * @param status the HTTP status to check
	 * @return true if the status is defined as successful; else false
	 */
	public static final boolean isStatusSuccessful(int status) {
		return status >= 200 && status < 300;
	}

	/**
	 * Checks if the HTTP status code is defined as a redirection message (300-399).
	 * 
	 * @param status the HTTP status to check
	 * @return true if the status is defined as a redirection; else false
	 */
	public static final boolean isStatusRedirection(int status) {
		return status >= 300 && status < 400;
	}

	/**
	 * Checks if the HTTP status code is defined as a client error response
	 * (400-499).
	 * 
	 * @param status the HTTP status to check
	 * @return true if the status is defined as a client error; else false
	 */
	public static final boolean isStatusClientError(int status) {
		return status >= 400 && status < 500;
	}

	/**
	 * Checks if the HTTP status code is defined as a server error response
	 * (500-599).
	 * 
	 * @param status the HTTP status to check
	 * @return true if the status is defined as a server error; else false
	 */
	public static final boolean isStatusServerError(int status) {
		return status >= 500 && status < 600;
	}

	/**
	 * Checks if the HTTP status code is a client error or a server error.
	 * 
	 * @param status the HTTP status to check
	 * @return true if the status is either a client error or server error; else
	 *         false
	 * @see #isStatusClientError(int)
	 * @see #isStatusServerError(int)
	 */
	public static final boolean isStatusError(int status) {
		return isStatusClientError(status) || isStatusServerError(status);
	}

	@Override
	public String toString() {
		return this.code + " " + this.description;
	}

	/**
	 * Checks if the HTTP status code is defined as a information response
	 * (100-199).
	 * 
	 * @return true if the status is defined as informational; else false
	 * @see #isStatusInformational(int)
	 */
	public boolean isInformational() {
		return isStatusInformational(this.code);
	}

	/**
	 * Checks if the HTTP status code is defined as a successful response (200-299).
	 * 
	 * @return true if the status is defined as successful; else false
	 * @see #isStatusSuccessful(int)
	 */
	public boolean isSuccessful() {
		return isStatusSuccessful(this.code);
	}

	/**
	 * Checks if the HTTP status code is defined as a redirection message (300-399).
	 * 
	 * @return true if the status is defined as a redirection; else false
	 * @see #isStatusRedirection(int)
	 */
	public boolean isRedirection() {
		return isStatusRedirection(this.code);
	}

	/**
	 * Checks if the HTTP status code is defined as a client error response
	 * (400-499).
	 * 
	 * @return true if the status is defined as a client error; else false
	 * @see #isStatusClientError(int)
	 */
	public boolean isClientError() {
		return isStatusClientError(this.code);
	}

	/**
	 * Checks if the HTTP status code is defined as a server error response
	 * (500-599).
	 * 
	 * @return true if the status is defined as a server error; else false
	 * @see #isStatusServerError(int)
	 */
	public boolean isServerError() {
		return isStatusServerError(this.code);
	}

	/**
	 * Checks if the HTTP status code is a client error or a server error.
	 * 
	 * @return true if the status is either a client error or server error; else
	 *         false
	 * @see #isStatusError(int)
	 */
	public boolean isError() {
		return isStatusError(this.code);
	}

}
