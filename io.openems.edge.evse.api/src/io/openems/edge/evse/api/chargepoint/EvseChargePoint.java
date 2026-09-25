package io.openems.edge.evse.api.chargepoint;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.MeterType;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

public interface EvseChargePoint extends ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Charge-Point is ready for charging.
		 * 
		 * <ul>
		 * <li>Cable is plugged on Charge-Point and Electric-Vehicle
		 * <li>RFID authorization is OK (if required)
		 * <li>There are no known errors
		 * <li>...
		 * </ul>
		 */
		IS_READY_FOR_CHARGING(Doc.of(OpenemsType.BOOLEAN)), //

		/**
		 * Externally requested maximum charge power in [W].
		 * 
		 * <p>
		 * This write-only channel is intended for external controllers like evcc. It is
		 * evaluated by {@code Evse.Controller.Single} only when configured in external
		 * mode.
		 */
		SET_MAXIMUM_CHARGE_POWER(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.text("Externally requested maximum charge power in [W]. Evaluated by Evse.Controller.Single in external mode.")), //

		/**
		 * Externally requested charging enable flag.
		 * 
		 * <p>
		 * This write-only channel is intended for external controllers like evcc. It is
		 * evaluated by {@code Evse.Controller.Single} only when configured in external
		 * mode. A missing value is treated as enabled.
		 */
		SET_CHARGING_ENABLED(new BooleanDoc() //
				.accessMode(AccessMode.WRITE_ONLY) //
				.text("Externally requested charging enable flag. Evaluated by Evse.Controller.Single in external mode.")) //
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
	 * Gets the {@link ChargePointAbilities}.
	 * 
	 * @return the {@link ChargePointAbilities}
	 */
	public ChargePointAbilities getChargePointAbilities();

	/**
	 * Apply {@link ChargePointActions}.
	 * 
	 * @param actions the {@link ChargePointActions}
	 */
	public void apply(ChargePointActions actions);

	/**
	 * Is this Charge-Point installed according to standard or rotated wiring?. See
	 * {@link PhaseRotation} for details.
	 *
	 * @return the {@link PhaseRotation}.
	 */
	public PhaseRotation getPhaseRotation();

	/**
	 * Is this {@link EvseChargePoint} read-only or read-write?.
	 *
	 * @return true for read-only
	 */
	public boolean isReadOnly();

	/**
	 * Gets the {@link MeterType} of an {@link EvseChargePoint}
	 * {@link ElectricityMeter}.
	 * 
	 * @return the {@link MeterType}
	 */
	@Override
	public default MeterType getMeterType() {
		return this.isReadOnly() //
				? MeterType.CONSUMPTION_METERED //
				: MeterType.MANAGED_CONSUMPTION_METERED;
	}

	/**
	 * Gets the Channel for {@link ChannelId#IS_READY_FOR_CHARGING}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getIsReadyForChargingChannel() {
		return this.channel(ChannelId.IS_READY_FOR_CHARGING);
	}

	/**
	 * Gets the Plug-Status of the Charge Point. See
	 * {@link ChannelId#IS_READY_FOR_CHARGING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getIsReadyForCharging() {
		return this.getIsReadyForChargingChannel().value().orElse(false);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_MAXIMUM_CHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaximumChargePowerChannel() {
		return this.channel(ChannelId.SET_MAXIMUM_CHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGING_ENABLED}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetChargingEnabledChannel() {
		return this.channel(ChannelId.SET_CHARGING_ENABLED);
	}
}
