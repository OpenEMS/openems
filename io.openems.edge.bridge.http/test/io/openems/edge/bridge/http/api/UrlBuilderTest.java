package io.openems.edge.bridge.http.api;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

public class UrlBuilderTest {

	@Test
	public void testParse() {
		final var rawUrl = "https://openems.io:443/path?key=value#fragment";
		final var parsedUrl = UrlBuilder.parse(rawUrl);
		assertEquals(rawUrl, parsedUrl.toEncodedString());
	}

	@Test
	public void testParseNoQueryParams() {
		final var rawUrl = "https://openems.io:443/path#fragment";
		final var parsedUrl = UrlBuilder.parse(rawUrl);
		assertEquals(rawUrl, parsedUrl.toEncodedString());
	}

	@Test
	public void testScheme() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io");

		assertEquals("https://openems.io", url.toEncodedString());
		assertEquals("http://openems.io", url.withScheme("http").toEncodedString());
	}

	@Test
	public void testHost() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io");

		assertEquals("https://openems.io", url.toEncodedString());
		assertEquals("https://better.openems.io", url.withHost("better.openems.io").toEncodedString());
	}

	@Test
	public void testPort() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPort(443);

		assertEquals("https://openems.io:443", url.toEncodedString());
		assertEquals("https://openems.io:445", url.withPort(445).toEncodedString());
	}

	@Test
	public void testPath() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPath("/path");

		assertEquals("https://openems.io/path", url.toEncodedString());
		assertEquals("https://openems.io/path/abc", url.withPath("/path/abc").toEncodedString());
		assertEquals("https://openems.io/withoutslash", url.withPath("withoutslash").toEncodedString());
	}

	@Test
	public void testQueryParameter() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withQueryParam("key", "value");

		assertEquals("https://openems.io?key=value", url.toEncodedString());
		assertEquals("https://openems.io?key=otherValue", url.withQueryParam("key", "otherValue").toEncodedString());
	}

	@Test
	public void testFragment() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withFragment("myFragment");

		assertEquals("https://openems.io#myFragment", url.toEncodedString());
		assertEquals("https://openems.io#myOtherFragment", url.withFragment("myOtherFragment").toEncodedString());
		assertEquals("https://openems.io#with", url.withFragment("#with").toEncodedString());
	}

	@Test
	public void testToUri() {
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
	public void testToEncodedString() {
		final var url = UrlBuilder.create() //
				.withScheme("https") //
				.withHost("openems.io") //
				.withPort(443) //
				.withPath("/path") //
				.withQueryParam("key", "va lu+e") //
				.withFragment("fragment");

		assertEquals("https://openems.io:443/path?key=va%20lu%2Be#fragment", url.toEncodedString());
	}

}
