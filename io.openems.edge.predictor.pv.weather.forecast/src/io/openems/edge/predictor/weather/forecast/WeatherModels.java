package io.openems.edge.predictor.weather.forecast;

public enum WeatherModels {
	UNDEFINED("undefined", ""),
	BestMatch("best match", "&models=best_match"),
	DWD("DWD", "&models=icon_d2");
	
	private final String displayName;
	private final String weatherModel;
	
	WeatherModels(String displayName, String weatherModel) {
        this.displayName = displayName;
        this.weatherModel = weatherModel;

    }	
    public String getDisplayName() {
        return this.displayName;
    }

    public String getWeatherModel() {
        return this.weatherModel;
    }	
}


