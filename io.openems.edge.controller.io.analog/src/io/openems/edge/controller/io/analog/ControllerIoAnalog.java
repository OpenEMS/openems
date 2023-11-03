package io.openems.edge.controller.io.analog;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerIoAnalog extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CUMULATED_ACTIVE_ENERGY(Doc.of(LONG)//
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(HIGH)) //
		; //

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
