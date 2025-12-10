package io.openems.edge.phoenixcontact.plcnext.utils;

public final class PlcNextUrlStringHelper {

	private PlcNextUrlStringHelper() {
	}

	public static String buildUrlString(String baseUrl, String path) {
		StringBuilder urlStringBuilder = new StringBuilder(baseUrl);

		if (baseUrl.endsWith("/") && path.startsWith("/")) {
			urlStringBuilder.deleteCharAt(baseUrl.length() - 1);
		}
		if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
			urlStringBuilder.append("/");
		}
		urlStringBuilder.append(path);

		return urlStringBuilder.toString();
	}
}
