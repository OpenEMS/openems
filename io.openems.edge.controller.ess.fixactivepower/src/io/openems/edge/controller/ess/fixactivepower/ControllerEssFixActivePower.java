package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedulable;

public interface ControllerEssFixActivePower extends Controller, OpenemsComponent, Schedulable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CUMULATED_ACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH));

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
