package io.openems.common.bridge.http.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class UrlBuilderTest {

	@Test
	void testInitialPort() {
		final var urlWithPort = UrlBuilder.parse("https://openems.io:443");
		assertEquals(443, urlWithPort.port().intValue());

		final var urlWithoutPort = UrlBuilder.parse("https://openems.io");
		assertNull(urlWithoutPort.port());
	}

	@Test
	void testParse() {
		final var rawUrl = "https://openems.io:443/path?key=value#fragment";
		final var parsedUrl = UrlBuilder.parse(rawUrl);
		assertEquals(rawUrl, parsedUrl.toEncodedString());
	}

	@Test
	void testParseNoQueryParams() {
		final var rawUrl = "https://openems.io:443/path#fragment";
		final var parsedUrl = UrlBuilder.parse(rawUrl);
		assertEquals(rawUrl, parsedUrl.toEncodedString());
	}

	@Test
	void testScheme() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io");

		assertEquals("https://openems.io", url.toEncodedString());
		assertEquals("http://openems.io", url.withScheme("http").toEncodedString());
	}

	@Test
	void testHost() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io");

		assertEquals("https://openems.io", url.toEncodedString());
		assertEquals("https://better.openems.io", url.withHost("better.openems.io").toEncodedString());
	}

	@Test
	void testPort() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPort(443);

		assertEquals("https://openems.io:443", url.toEncodedString());
		assertEquals("https://openems.io:445", url.withPort(445).toEncodedString());
	}

	@Test
	void testPath() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPath("/path");

		assertEquals("https://openems.io/path", url.toEncodedString());
		assertEquals("https://openems.io/path/abc", url.withPath("/path/abc").toEncodedString());
		assertEquals("https://openems.io/withoutslash", url.withPath("withoutslash").toEncodedString());
	}

	@Test
	void testQueryParameter() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withQueryParam("key", "value");

		assertEquals("https://openems.io?key=value", url.toEncodedString());
		assertEquals("https://openems.io?key=otherValue", url.withQueryParam("key", "otherValue").toEncodedString());
	}

	@Test
	void testFragment() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withFragment("myFragment");

		assertEquals("https://openems.io#myFragment", url.toEncodedString());
		assertEquals("https://openems.io#myOtherFragment", url.withFragment("myOtherFragment").toEncodedString());
		assertEquals("https://openems.io#with", url.withFragment("#with").toEncodedString());
	}

	@Test
	void testToUri() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPort(443) //
				.withPath("/path") //
				.withQueryParam("key", "value") //
				.withFragment("fragment");

		assertEquals(URI.create("https://openems.io:443/path?key=value#fragment"), url.toUri());
	}

	@Test
	void testToEncodedString() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPort(443) //
				.withPath("/path") //
				.withQueryParam("key", "va lu+e") //
				.withFragment("fragment");

		assertEquals("https://openems.io:443/path?key=va%20lu%2Be#fragment", url.toEncodedString());
	}

	@Test
	void testEncodeFormUrlencodedBody() {
		final var value = UrlBuilder.encodeFormUrlencodedBody(Map.of("key", "va lu+e"));

		assertEquals("key=va%20lu%2Be", value);
	}

	@Test
	void testDecodeFormUrlencodedBody() {
		final var values = UrlBuilder.decodeFormUrlencodedBody("key=va%20lu%2Be");

		assertEquals(1, values.size());
		assertEquals("va lu+e", values.get("key"));
	}
}
