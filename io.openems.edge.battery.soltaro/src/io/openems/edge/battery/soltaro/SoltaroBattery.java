package io.openems.edge.battery.soltaro;

import java.time.ZoneOffset;

import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface SoltaroBattery extends Battery {

	public static final ZoneOffset ZONE_OFFSET = ZoneOffset.UTC;
	public static final long NANOS = 0;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
		NOT_ACTIVE_SINCE(Doc.of(OpenemsType.LONG)) //
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

	default Channel<ChargeIndication> getChargeIndication() {
		return this.channel(ChannelId.CHARGE_INDICATION);
	}

	default Channel<Long> getNotActiveSince() {
		return this.channel(ChannelId.NOT_ACTIVE_SINCE);
	}
}
