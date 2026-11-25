package io.openems.common.bridge.http.api;

public final class HttpMediaType {

	public static final class Application {

		private static final String PREFIX = "application/";

		public static final String JSON = PREFIX + "json";
		public static final String XML = PREFIX + "xml";
		public static final String PDF = PREFIX + "pdf";
		public static final String ZIP = PREFIX + "zip";
		public static final String GZIP = PREFIX + "gzip";
		public static final String OCTET_STREAM = PREFIX + "octet-stream";
		public static final String X_WWW_FORM_URLENCODED = PREFIX + "x-www-form-urlencoded";

		private Application() {
		}
	}

	public static final class Text {

		private static final String PREFIX = "text/";

		public static final String PLAIN = PREFIX + "plain";
		public static final String HTML = PREFIX + "html";
		public static final String CSS = PREFIX + "css";
		public static final String CSV = PREFIX + "csv";
		public static final String JAVASCRIPT = PREFIX + "javascript";
		public static final String MARKDOWN = PREFIX + "markdown";

		private Text() {
		}
	}

	private HttpMediaType() {
	}
}
