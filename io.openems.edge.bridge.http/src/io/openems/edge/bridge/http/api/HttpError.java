package io.openems.edge.bridge.http.api;

import io.openems.common.types.HttpStatus;

public abstract sealed class HttpError extends Exception {

	private static final long serialVersionUID = 6341345161164781738L;

	private HttpError(String message) {
		super(message);
	}

	private HttpError(Throwable cause) {
		super(cause);
	}

	public static final class ResponseError extends HttpError {

		private static final long serialVersionUID = -5382307294288467972L;

		/**
		 * Creates a {@link HttpError#ResponseError} for a not found error. The
		 * predefined values are for status "404" and message "Not Found".
		 * 
		 * @return the error
		 */
		public static ResponseError notFound() {
			return new ResponseError(HttpStatus.NOT_FOUND, null);
		}

		public final HttpStatus status;
		public final String body;

		public ResponseError(HttpStatus status, String body) {
			super("Http " + status + (body != null ? ", Body=" + body : ""));
			this.status = status;
			this.body = body;
		}

		@Override
		public String toString() {
			return "ResponseError [status=" + this.status + ", body=" + this.body + "]";
		}

	}

	public static final class UnknownError extends HttpError {

		private static final long serialVersionUID = 5683236662459434998L;

		public UnknownError(Throwable cause) {
			super(cause);
		}

	}

}
