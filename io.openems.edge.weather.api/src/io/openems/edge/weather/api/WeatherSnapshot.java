package io.openems.edge.weather.api;

public record WeatherSnapshot(//
		double globalHorizontalIrradiance, //
		double directNormalIrradiance, //
		double temperature, //
		int weatherCode //
) {
}
