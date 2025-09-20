package io.openems.edge.bridge.modbus.api;

import java.net.InetAddress;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Doc;

@ProviderType
public interface BridgeModbusTcp extends BridgeModbus {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
	 * Gets the IP address.
	 *
	 * @return the IP address
	 */
	public InetAddress getIpAddress();

}
