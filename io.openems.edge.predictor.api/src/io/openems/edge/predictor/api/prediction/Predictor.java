package io.openems.edge.predictor.api.prediction;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a {@link Prediction}.
 */
@ProviderType
public interface Predictor extends OpenemsComponent {

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
	 * Gets a {@link Prediction} for the given {@link ChannelAddress}.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Prediction}
	 */
	public Prediction getPrediction(ChannelAddress channelAddress);

}
