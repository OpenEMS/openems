package io.openems.edge.controller.ess.soc.pvcurtail;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface PvCurtailBySoc extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Master PPC reactive power setpoint at POI (QAC_PC_RX) [var].
		 * WRITE this from Node-RED / REST / UI. Latched until overwritten.
		 * REST: POST /rest/channel/ctrlSocPvCurtail0/QacPcRx  {"value": 132000}
		 */
		QAC_PC_RX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.READ_WRITE) //
				.text("Master PPC Q setpoint at POI [var]")),

		/** Net-zero offset that was added this cycle [var] (day +200k / night -50k). */
		Q_OFFSET_USED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/** Setpoint + offset, before the PV/BESS split [var]. */
		Q_TOTAL_CMD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/** Final (slew-limited) Q command sent to PV [var]. */
		PV_Q_CMD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/** Final (slew-limited) Q command sent to BESS [var]. */
		BESS_Q_CMD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/** PV Q capacity used for the split (positive side / inductive) [var]. */
		PV_Q_CAP_POS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/** PV Q capacity used for the split (negative side / capacitive) [var]. */
		PV_Q_CAP_NEG(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)),

		/** Measured Q at POI meter [var], for the setpoint-vs-actual plot. */
		POI_Q_MEASURED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE));

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