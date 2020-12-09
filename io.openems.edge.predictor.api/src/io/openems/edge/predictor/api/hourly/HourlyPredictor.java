package io.openems.edge.predictor.api.hourly;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a prediction for the next 24 h.
 */
@ProviderType
@Deprecated
public interface HourlyPredictor extends OpenemsComponent {

	/**
	 * Gives a prediction for the next 24 h; one value per hour.
	 * 
	 * <p>
	 * E.g. if called at 10:30, the first value stands for 10 to 11; second value
	 * for 11 to 12.
	 * 
	 * @return the {@link HourlyPrediction}
	 */
	HourlyPrediction get24hPrediction();

}
