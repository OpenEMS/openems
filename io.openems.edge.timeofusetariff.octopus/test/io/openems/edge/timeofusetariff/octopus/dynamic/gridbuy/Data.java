package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builders for synthetic Kraken API responses used in unit tests.
 */
final class Data {

	static final String GRAPHQL_AUTHORIZATION_ERROR = """
			{"errors":[{"message":"Signature of the JWT has expired.",
			  "extensions":{"errorType":"AUTHORIZATION","errorCode":"KT-CT-1124"}}],
			 "data":null}""";

	static final String GRAPHQL_RATE_LIMIT_ERROR = """
			{"errors":[{"message":"Too many requests.",
			  "extensions":{"errorType":"APPLICATION","errorCode":"KT-CT-1199"}}],
			 "data":null}""";

	static final String GRAPHQL_SERVER_ERROR = """
			{"errors":[{"message":"Something went wrong.",
			  "extensions":{"errorType":"APPLICATION","errorCode":"KT-CT-4321"}}],
			 "data":null}""";

	private Data() {
	}

	static String token(String token) {
		return """
				{"data":{"obtainKrakenToken":{"token":"%s"}}}""".formatted(token);
	}

	static String forecast(String... agreements) {
		return """
				{"data":{"account":{"properties":[{"electricityMalos":[{"agreements":[%s]}]}]}}}"""
				.formatted(String.join(",", agreements));
	}

	static String agreement(String... slots) {
		return """
				{"unitRateForecast":[%s]}""".formatted(Arrays.stream(slots).collect(Collectors.joining(",")));
	}

	static String slot(String from, String to, String price) {
		return """
				{"validFrom":"%s","validTo":"%s",
				 "unitRateInformation":{"rates":[{"latestGrossUnitRateCentsPerKwh":%s}]}}"""
				.formatted(from, to, price);
	}
}
