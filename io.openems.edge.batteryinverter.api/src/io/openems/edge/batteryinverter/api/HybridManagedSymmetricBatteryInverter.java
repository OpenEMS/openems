package io.openems.edge.batteryinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

/**
 * Represents a Hybrid Symmetric Battery-Inverter - as part of a
 * {@link HybridEss} - that can be controlled.
 */
@ProviderType
public interface HybridManagedSymmetricBatteryInverter
		extends ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * DC Discharge Power.
		 * 
		 * <ul>
		 * <li>Interface: HybridManagedSymmetricBatteryInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>This is the
		 * {@link io.openems.edge.ess.api.SymmetricBatteryInverter.ChannelId#ACTIVE_POWER}
		 * minus
		 * {@link io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId#ACTUAL_POWER},
		 * i.e. the power that is actually charged to or discharged from the battery.
		 * </ul>
		 */
		DC_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * DC Charge Energy.
		 * 
		 * <ul>
		 * <li>Interface: HybridEss
		 * <li>Type: Long
		 * <li>Unit: Wh
		 * </ul>
		 */
		DC_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * DC Discharge Energy.
		 * 
		 * <ul>
		 * <li>Interface: HybridEss
		 * <li>Type: Long
		 * <li>Unit: Wh
		 * </ul>
		 */
		DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Surplus Power of the {@link EssDcCharger}s of this
	 * {@link HybridManagedSymmetricBatteryInverter}.
	 * 
	 * <p>
	 * This value is usually calculated from the
	 * {@link EssDcCharger#getActualPower()} when the battery is full
	 * 
	 * @return the surplus power, or 'null' if there is no surplus power
	 */
	public Integer getSurplusPower();
}
