package io.openems.edge.io.shelly.common;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Common interface for all Shelly devices.
 * 
 * <p>Provides standard channels for authentication warnings and device status
 * that are common across all Shelly devices.
 */
public interface ShellyCommon extends OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Authentication Warning.
		 *
		 * <ul>
		 * <li>Interface: ShellyCommon
		 * <li>Type: State
		 * <li>Level: WARNING
		 * </ul>
		 */
		AUTH_ENABLED_WARNING(Doc.of(Level.WARNING) //
				.text("Shelly authentication is enabled. Please disable authentication in Shelly settings for OpenEMS to work properly.")),
		
		/**
		 * Device Generation Info.
		 *
		 * <ul>
		 * <li>Interface: ShellyCommon
		 * <li>Type: String
		 * <li>Description: Shows the device generation (gen field from /shelly endpoint)
		 * </ul>
		 */
		DEVICE_GENERATION(Doc.of(io.openems.common.types.OpenemsType.STRING) //
				.text("Device generation"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#AUTH_ENABLED_WARNING}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getAuthEnabledWarningChannel() {
		return this.channel(ChannelId.AUTH_ENABLED_WARNING);
	}

	/**
	 * Gets the Authentication Enabled Warning State. See
	 * {@link ChannelId#AUTH_ENABLED_WARNING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAuthEnabledWarning() {
		return this.getAuthEnabledWarningChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#AUTH_ENABLED_WARNING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setAuthEnabledWarning(boolean value) {
		this.getAuthEnabledWarningChannel().setNextValue(value);
	}
}