package io.openems.edge.evse.chargepoint.mennekes.common;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Mennekes Amtron Professional charging protocol interface.
 * 
 * <p>
 * Defines the interface for Mennekes Amtron Professional
 */
public interface Mennekes extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Apply charge current limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		SET_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.accessMode(WRITE_ONLY)), //

		EMS_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)), //
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
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyCurrentLimitChannel() {
		return this.channel(ChannelId.SET_CURRENT_LIMIT);
	}

}
