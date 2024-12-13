package io.openems.edge.bridge.http.api;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Simple URL Builder class to build URLs with correctly encoded query
 * parameter. This class is immutable.
 * 
 * <p>
 * Example Usage:
 * 
 * <pre>
 * <code>
 * final var url = UrlBuilder.create() //
		.withScheme("https") //
		.withHost("openems.io") //
		.withPort(443) //
		.withPath("/path/to") //
		.withQueryParam("key", "value") //
		.withFragment("fragment") //
		// get final result
		.toUri() // to get a URI Object
		.toUrl() // to get a URL Object
		.toEncodedString() // to get the URL as a encoded String
 * </code>
 * or parse from a string
 * <code>
 * final var url = UrlBuilder.from("https://openems.io:443/path?key=value#fragment");
 * </code>
 * </pre>
 * 
 * <p>
 * URL-Schema: <code>
 * scheme://host:port/path?queryParams#fragment
 * </code>
 */
public final class UrlBuilder {

	/**
	 * Parses a raw uri string to the url parts.
	 * 
	 * @param uriString the raw string to parse
	 * @return the {@link UrlBuilder} with the parts of th uri
	 */
	public static UrlBuilder parse(String uriString) {
		final var uri = URI.create(uriString);

		final var query = uri.getQuery();
		final var queryParams = query == null ? Collections.<String, String>emptyMap()
				: Stream.of(uri.getQuery().split("&")) //
						.map(t -> t.split("=")) //
						.collect(toUnmodifiableMap(t -> t[0], t -> t[1]));
		return new UrlBuilder(//
				uri.getScheme(), //
				uri.getHost(), //
				uri.getPort(), //
				uri.getPath(), //
				queryParams, //
				uri.getFragment() //
		);
	}

	/**
	 * Creates a new {@link UrlBuilder}.
	 * 
	 * @return the new {@link UrlBuilder} instance
	 */
	public static UrlBuilder create() {
		return new UrlBuilder(null, null, null, null, Collections.emptyMap(), null);
	}

	private final String scheme;
	private final String host;
	private final Integer port;
	private final String path;
	private final Map<String, String> queryParams;
	private final String fragment;

	private UrlBuilder(//
			String scheme, //
			String host, //
			Integer port, //
			String path, //
			Map<String, String> queryParams, //
			String fragment //
	) {
		this.scheme = scheme;
		this.host = host;
		this.port = port;
		this.path = path;
		this.queryParams = queryParams;
		this.fragment = fragment;
	}

	/**
	 * Creates a copy of the current {@link UrlBuilder} with the new scheme.
	 * 
	 * @param scheme the new scheme
	 * @return the copy of the {@link UrlBuilder} with the new scheme
	 */
	public UrlBuilder withScheme(String scheme) {
		return new UrlBuilder(scheme, this.host, this.port, this.path, this.queryParams, this.fragment);
	}

	/**
	 * Creates a copy of the current {@link UrlBuilder} with the new host.
	 * 
	 * @param host the new host
	 * @return the copy of the {@link UrlBuilder} with the new host
	 */
	public UrlBuilder withHost(String host) {
		return new UrlBuilder(this.scheme, host, this.port, this.path, this.queryParams, this.fragment);
	}

	/**
	 * Creates a copy of the current {@link UrlBuilder} with the new port.
	 * 
	 * @param port the new port
	 * @return the copy of the {@link UrlBuilder} with the new port
	 */
	public UrlBuilder withPort(int port) {
		if (port < 0) {
			throw new IllegalArgumentException("Property 'port' must not be smaller than '0'.");
		}
		return new UrlBuilder(this.scheme, this.host, port, this.path, this.queryParams, this.fragment);
	}

	/**
	 * Creates a copy of the current {@link UrlBuilder} with the new path.
	 * 
	 * @param path the new path
	 * @return the copy of the {@link UrlBuilder} with the new path
	 */
	public UrlBuilder withPath(String path) {
		return new UrlBuilder(this.scheme, this.host, this.port, path, this.queryParams, this.fragment);
	}

	/**
	 * Creates a copy of the current {@link UrlBuilder} with the new query parameter
	 * added.
	 * 
	 * @param key   the key of the new query parameter
	 * @param value the value of the new query parameter
	 * @return the copy of the {@link UrlBuilder} with the new query parameter added
	 */
	public UrlBuilder withQueryParam(String key, String value) {
		Map<String, String> newQueryParams = new HashMap<>(this.queryParams);
		newQueryParams.put(key, value);
		return new UrlBuilder(this.scheme, this.host, this.port, this.path, Collections.unmodifiableMap(newQueryParams),
				this.fragment);
	}

	/**
	 * Creates a copy of the current {@link UrlBuilder} with the new fragment.
	 * 
	 * @param fragment the new fragment
	 * @return the copy of the {@link UrlBuilder} with the new fragment
	 */
	public UrlBuilder withFragment(String fragment) {
		return new UrlBuilder(this.scheme, this.host, this.port, this.path, this.queryParams, fragment);
	}

	/**
	 * Creates a {@link URI} from this object.
	 * 
	 * @return the {@link URI}
	 */
	public URI toUri() {
		return URI.create(this.toEncodedString());
	}

	/**
	 * Creates a {@link URI} from this object.
	 * 
	 * @return the {@link URI}
	 * @throws MalformedURLException If a protocol handler for the URL could not be
	 *                               found, or if some other error occurred while
	 *                               constructing the URL
	 */
	public URL toUrl() throws MalformedURLException {
		return this.toUri().toURL();
	}

	/**
	 * Creates an encoded string url from this object.
	 * 
	 * <p>
	 * Note: does not check if the url is valid. To Check if it is valid use
	 * {@link #toUrl()}
	 * 
	 * @return the encoded url
	 */
	public String toEncodedString() {
		final var url = new StringBuilder();

		url.append(this.scheme);
		url.append("://");
		url.append(this.host);

		if (this.port != null) {
			url.append(":");
			url.append(this.port);
		}

		if (this.path != null && !this.path.isEmpty()) {
			if (!this.path.startsWith("/")) {
				url.append("/");
			}
			url.append(this.path);
		}

		if (!this.queryParams.isEmpty()) {
			var query = this.queryParams.entrySet().stream() //
					.map(t -> encode(t.getKey()) + "=" + encode(t.getValue())) //
					.collect(joining("&", "?", ""));
			url.append(query);
		}

		if (this.fragment != null && !this.fragment.isEmpty()) {
			if (!this.fragment.startsWith("#")) {
				url.append("#");
			}
			url.append(this.fragment);
		}

		return url.toString();
	}

	// Helper method to URL-encode values
	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8) //
				.replace("+", "%20") // " " => "+" => "%20"
		;
	}
}
