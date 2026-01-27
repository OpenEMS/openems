package io.openems.edge.phoenixcontact.plcnext.common.utils;

/**
 * Helper class for URLs fragments
 */
public final class PlcNextUrlStringHelper {

	private PlcNextUrlStringHelper() {
	}

	/**
	 * Joins the given path with given base URL considering slashes where required
	 * 
	 * @param baseUrl	the base URL to join
	 * @param path		the path to join
	 * @return	joined base URL and path
	 */
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
