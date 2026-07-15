package io.openems.edge.simulator.evse.chargepoint;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.simulator.evse.chargepoint.enums.PhaseSwitchState;

public interface SimulatorEvseChargepoint extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PHASE_SWITCH_STATE(Doc.of(PhaseSwitchState.values())), //

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
	 * Gets the Channel for {@link ChannelId#PHASE_SWITCH_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getPhaseSwitchStateChannel() {
		return this.channel(ChannelId.PHASE_SWITCH_STATE);
	}

	/**
	 * Gets the {@link PhaseSwitchState}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default PhaseSwitchState getPhaseSwitchState() {
		return this.getPhaseSwitchStateChannel().getNextValue().asEnum();
	}

}
