package io.openems.edge.heat.mypv;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heat.mypv.statemachine.StateMachine;

public interface HeatMyPv extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(ChannelMode.values())//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current mode of the device")), //
		STATE_MACHINE(Doc.of(StateMachine.State.values())//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current state-machine state")), //
		FAST_HEAT_POWER_NOT_APPLIED(Doc.of(Level.INFO)//
				.translationKey(HeatMyPv.class, "FAST_HEAT_POWER_NOT_APPLIED")), //
		BOOST_ACTIVE(Doc.of(Level.FAULT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.translationKey(HeatMyPv.class, "BOOST_ACTIVE")); //

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
