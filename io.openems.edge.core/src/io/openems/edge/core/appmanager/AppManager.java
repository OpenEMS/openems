package io.openems.edge.core.appmanager;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

/**
 * A Service that manages OpenEMS Apps.
 */
public interface AppManager extends OpenemsComponent, JsonApi {

	public static final String SINGLETON_SERVICE_PID = "Core.AppManager";
	public static final String SINGLETON_COMPONENT_ID = "_appManager";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		WRONG_APP_CONFIGURATION(Doc.of(Level.WARNING) //
				.text("App-Manager configuration is wrong")), //
		DEFECTIVE_APP(Doc.of(Level.INFO) //
				// TODO should be a WARNING eventually
				.text("Defective App detected")), //
		;

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
	 * Gets the Channel for {@link ChannelId#WRONG_APP_CONFIGURATION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWrongAppConfigurationChannel() {
		return this.channel(ChannelId.WRONG_APP_CONFIGURATION);
	}

	/**
	 * Gets the Wrong-App-Configuration Warning State. See
	 * {@link ChannelId#WRONG_APP_CONFIGURATION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWrongAppConfiguration() {
		return this.getWrongAppConfigurationChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WRONG_APP_CONFIGURATION} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWrongAppConfiguration(boolean value) {
		this.getWrongAppConfigurationChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEFECTIVE_APP}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDefectiveAppChannel() {
		return this.channel(ChannelId.DEFECTIVE_APP);
	}

	/**
	 * Gets the Defective-App Warning State. See {@link ChannelId#DEFECTIVE_APP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getDefectiveApp() {
		return this.getDefectiveAppChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DEFECTIVE_APP}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDefectiveApp(boolean value) {
		this.getDefectiveAppChannel().setNextValue(value);
	}

}
