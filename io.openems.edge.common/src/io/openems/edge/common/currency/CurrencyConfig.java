package io.openems.edge.common.currency;

import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.Meta.ChannelId;

/**
 * The {@link ChannelId#CURRENCY} mandates the selection of the 'currency'
 * configuration property of this specific type. Subsequently, this selected
 * property is transformed into the corresponding {@link Currency} type before
 * being written through {@link Meta#_setCurrency(Currency)}.
 */
public enum CurrencyConfig {
	/**
	 * Euro.
	 */
	EUR,
	/**
	 * Swedish Krona.
	 */
	SEK,
}
