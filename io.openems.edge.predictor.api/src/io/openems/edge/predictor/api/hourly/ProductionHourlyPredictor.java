package io.openems.edge.predictor.api.hourly;

/**
 * Provides a production prediction for the next 24 h; e.g. for a photovoltaics
 * installation.
 */
//TODO remove the ProductionHourlyPredictor in favor of PredictorManager API
public interface ProductionHourlyPredictor extends HourlyPredictor {

}
