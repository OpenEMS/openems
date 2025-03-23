package io.openems.edge.predictor.solartariff;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a prediction for the next hours; one value hour; 72 values
 * in total.
 */
@ProviderType
public interface PredictorHours extends OpenemsComponent {

	/**
	 * Gets the Channel-Addresses for which this Predictor can provide a prediction.
	 * 
	 * <p>
	 * The entries can contain wildcards to match multiple actual
	 * {@link ChannelAddress}es.
	 * 
	 * @return an array of {@link ChannelAddress}es
	 */
	public ChannelAddress[] getChannelAddresses();

	/**
	 * Gives a prediction for the next hours for the given {@link ChannelAddress};
	 * one value per hour; 24 values in total.
	 * 
	 * <p>
	 * E.g. if called at 10:05, the first value stands for 10:00 to 11:00; second
	 * value for 11:00 to 12:00.
	 * 
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link PredictionHours}
	 */
	public PredictionHours getHoursPrediction(ChannelAddress channelAddress);

}