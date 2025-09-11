package io.openems.edge.meter.ddsu666;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;


public interface MeterDDSU666 extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		POWER_FACTOR(Doc.of(OpenemsType.FLOAT) //
	            .unit(Unit.NONE) //
	            .text("Power Factor"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
