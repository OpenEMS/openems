package io.openems.common.bridge.http.api;

public final class HttpMediaType {

	public static final class Application {

		public static final String JSON = "application/json";
		public static final String XML = "application/xml";
		public static final String PDF = "application/pdf";
		public static final String ZIP = "application/zip";
		public static final String GZIP = "application/gzip";
		public static final String OCTET_STREAM = "application/octet-stream";
		public static final String X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

		private Application() {
		}
	}

	public static final class Text {

		public static final String PLAIN = "text/plain";
		public static final String HTML = "text/html";
		public static final String CSS = "text/css";
		public static final String CSV = "text/csv";
		public static final String JAVASCRIPT = "text/javascript";
		public static final String MARKDOWN = "text/markdown";

		private Text() {
		}
	}

	private HttpMediaType() {
	}
}
