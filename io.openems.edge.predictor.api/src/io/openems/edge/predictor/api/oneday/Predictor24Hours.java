package io.openems.edge.predictor.api.oneday;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a prediction for the next 24 h; one value per 15 minutes; 96 values
 * in total.
 */
@ProviderType
public interface Predictor24Hours extends OpenemsComponent {

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
	 * Gives a prediction for the next 24 h for the given {@link ChannelAddress};
	 * one value per 15 minutes; 96 values in total.
	 *
	 * <p>
	 * E.g. if called at 10:05, the first value stands for 10:00 to 10:15; second
	 * value for 10:15 to 10:30.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Prediction24Hours}
	 */
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress);

}
