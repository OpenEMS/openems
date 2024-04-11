package io.openems.edge.ess.sma.stpxx3se.battery;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.sma.enums.BatteryState;
import io.openems.edge.ess.sma.enums.SetControlMode;

public interface SmaBattery extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// 30955
		BAT_STATUS(Doc.of(BatteryState.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		
		SET_CONTROL_MODE(Doc.of(SetControlMode.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
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
	 * Applies the calculated power to the battery.
	 * 
	 * @param activePower the active power setpoint
	 * @param reactivePower the reactive power setpoint
	 * @throws OpenemsNamedException on error
	 */
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException;
}
