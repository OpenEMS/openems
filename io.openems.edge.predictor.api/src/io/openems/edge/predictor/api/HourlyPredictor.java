package io.openems.edge.predictor.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a prediction for the next 24 h.
 */
@ProviderType
public interface HourlyPredictor extends OpenemsComponent {

	/**
	 * Gives a prediction for the next 24 h; one value per hour.
	 * 
	 * E.g. if called at 10:30, the first value stands for 10 to 11; second value
	 * for 11 to 12.
	 * 
	 * @return
	 */
	HourlyPrediction get24hPrediction();

}
