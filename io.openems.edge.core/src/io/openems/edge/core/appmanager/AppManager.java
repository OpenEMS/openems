package io.openems.edge.core.appmanager;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A Service that manages OpenEMS Apps.
 */
public interface AppManager extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		WRONG_APP_CONFIGURATION(Doc.of(Level.WARNING) //
				.text("App-Manager configuration is wrong")), //
		DEFECTIVE_APP(Doc.of(Level.INFO) //
				// TODO should be a WARNING eventually
				.text("Defective App detected")), //
		APPS_NOT_SYNCED_WITH_BACKEND(Doc.of(Level.INFO) //
				.text("The currently installed apps are not the same as logged in the backend")), //
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

	public static final String SINGLETON_SERVICE_PID = "Core.AppManager";

	public static final String SINGLETON_COMPONENT_ID = "_appManager";

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DEFECTIVE_APP}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDefectiveApp(boolean value) {
		this.getDefectiveAppChannel().setNextValue(value);
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#APPS_NOT_SYNCED_WITH_BACKEND} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setAppsNotSyncedWithBackend(boolean value) {
		this.getAppsNotSyncedWithBackendChannel().setNextValue(value);
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
	 * Gets the Channel for {@link ChannelId#DEFECTIVE_APP}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDefectiveAppChannel() {
		return this.channel(ChannelId.DEFECTIVE_APP);
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
	 * Gets the Channel for {@link ChannelId#WRONG_APP_CONFIGURATION}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWrongAppConfigurationChannel() {
		return this.channel(ChannelId.WRONG_APP_CONFIGURATION);
	}

	/**
	 * Gets the Apps-Not-Synced-With-Backend Warning State. See
	 * {@link ChannelId#APPS_NOT_SYNCED_WITH_BACKEND}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAppsNotSyncedWithBackend() {
		return this.getAppsNotSyncedWithBackendChannel().value();
	}

	/**
	 * Gets the channel for {@link ChannelId#APPS_NOT_SYNCED_WITH_BACKEND}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getAppsNotSyncedWithBackendChannel() {
		return this.channel(ChannelId.APPS_NOT_SYNCED_WITH_BACKEND);
	}

}
