package io.openems.edge.bridge.eebus.api;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface EebusPeer extends OpenemsComponent {

	enum EebusPeerConnectionType implements OptionsEnum {
		NOT_CONNECTED(0),
		PEER_CONNECTED_TO_SERVER(1),
		CLIENT_CONNECTION_TO_PEER(2),
		;

		private final int value;

		private EebusPeerConnectionType(int value) {
			this.value = value;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name();
		}

		@Override
		public OptionsEnum getUndefined() {
			return NOT_CONNECTED;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		CONNECTION_FAILURE(Doc.of(Level.WARNING)),

		MISSING_TRUST(Doc.of(Level.WARNING)),

		TRUST_LEVEL(Doc.of(OpenemsType.INTEGER)), //

		REMOTE_IP(Doc.of(OpenemsType.STRING)), //

		CONNECTION_TYPE(Doc.of(EebusPeerConnectionType.values())), //

		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public String getSki();

	public boolean isValid();

}
