package io.openems.edge.io.phoenixcontact.utils;

import org.junit.Assert;
import org.junit.Test;

public class PlcNextUrlStringHelperTest {

	private static final String EXPECTED_RESULT = "https://test.local/baseUrl/hello/world";

	@Test
	public void testUrlConcatenationWithoutTrailingSlashOnBaseUrlAndWithoutLeadingSlashOnPath_Successfully() {
		String baseUrl = "https://test.local/baseUrl";
		String resourcePath = "hello/world";

		String result = PlcNextUrlStringHelper.buildUrlString(baseUrl, resourcePath);

		Assert.assertEquals(EXPECTED_RESULT, result);
	}

	@Test
	public void testUrlConcatenationWithTrailingSlashOnBaseUrlAndWithLeadingSlashOnPath_Successfully() {
		String baseUrl = "https://test.local/baseUrl/";
		String resourcePath = "/hello/world";

		String result = PlcNextUrlStringHelper.buildUrlString(baseUrl, resourcePath);

		Assert.assertEquals(EXPECTED_RESULT, result);
	}

	@Test
	public void testUrlConcatenationWithTrailingSlashOnBaseUrlAndWithoutLeadingSlashOnPath_Successfully() {
		String baseUrl = "https://test.local/baseUrl/";
		String resourcePath = "hello/world";

		String result = PlcNextUrlStringHelper.buildUrlString(baseUrl, resourcePath);

		Assert.assertEquals(EXPECTED_RESULT, result);
	}

	@Test
	public void testUrlConcatenationWithoutTrailingSlashOnBaseUrlAndWithLeadingSlashOnPath_Successfully() {
		String baseUrl = "https://test.local/baseUrl";
		String resourcePath = "/hello/world";

		String result = PlcNextUrlStringHelper.buildUrlString(baseUrl, resourcePath);

		Assert.assertEquals(EXPECTED_RESULT, result);
	}
}
