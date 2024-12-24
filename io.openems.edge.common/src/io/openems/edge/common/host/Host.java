package io.openems.edge.common.host;

import java.net.Inet4Address;
import java.util.List;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface Host extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.Host";
	public static final String SINGLETON_COMPONENT_ID = "_host";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DISK_IS_FULL(Doc.of(Level.INFO) //
				.text("Disk is full")), //
		HOSTNAME(Doc.of(OpenemsType.STRING)), //

		/**
		 * Operating System Version.
		 * 
		 * <p>
		 * e. g. 'Raspbian GNU/Linux 11 (bullseye)' or 'Windows 11'
		 */
		OS_VERSION(Doc.of(OpenemsType.STRING) //
				.text("Operating system version")), //
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
	 * Gets the Channel for {@link ChannelId#DISK_IS_FULL}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDiskIsFullChannel() {
		return this.channel(ChannelId.DISK_IS_FULL);
	}

	/**
	 * Gets the Disk is Full Warning State. See {@link ChannelId#DISK_IS_FULL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getDiskIsFull() {
		return this.getDiskIsFullChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DISK_IS_FULL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDiskIsFull(boolean value) {
		this.getDiskIsFullChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HOSTNAME}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getHostnameChannel() {
		return this.channel(ChannelId.HOSTNAME);
	}

	/**
	 * Gets the hostname. See {@link ChannelId#HOSTNAME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getHostname() {
		return this.getHostnameChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HOSTNAME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHostname(String value) {
		this.getHostnameChannel().setNextValue(value);
	}

	/**
	 * Gets all the IPs of the current system.
	 * 
	 * @return A list of all the IPs
	 * @throws OpenemsNamedException exception
	 */
	public List<Inet4Address> getSystemIPs() throws OpenemsNamedException;
	
	/**
	 * Gets the Channel for {@link ChannelId#OS_VERSION}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getOsVersionChannel() {
		return this.channel(ChannelId.OS_VERSION);
	}

	/**
	 * Gets the operating system version. See {@link ChannelId#OS_VERSION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getOsVersion() {
		return this.getOsVersionChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OS_VERSION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOsVersion(String value) {
		this.getOsVersionChannel().setNextValue(value);
	}

}
