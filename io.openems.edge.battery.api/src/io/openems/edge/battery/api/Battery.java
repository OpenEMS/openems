package io.openems.edge.battery.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Battery extends OpenemsComponent {

	// TODO State of Health, Temperature?
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * State of Charge
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOC(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		/**
		 * Min voltage for discharging
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		DISCHARGE_MIN_VOLTAGE(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT)),
		/**
		 * Max current for discharging
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		DISCHARGE_MAX_CURRENT(new Doc().type(OpenemsType.INTEGER).unit(Unit.AMPERE)),
		/**
		 * Maximal voltage for charging
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		CHARGE_MAX_VOLTAGE(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT)),
		/**
		 * Max current for charging
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		CHARGE_MAX_CURRENT(new Doc().type(OpenemsType.INTEGER).unit(Unit.AMPERE)),
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
	 * Gets the State of Charge in [%], range 0..100 %
	 * 
	 * @return
	 */
	default Channel<Integer> getSoc() {
		return this.channel(ChannelId.SOC);
	}

	/**
	 * Gets the min voltage for discharging
	 * 
	 * @return
	 */
	default Channel<Integer> getDischargeMinVoltage() {
		return this.channel(ChannelId.DISCHARGE_MIN_VOLTAGE);
	}
	
	/**
	 * Gets the max current for discharging
	 * 
	 * @return
	 */
	default Channel<Integer> getDischargeMaxCurrent() {
		return this.channel(ChannelId.DISCHARGE_MAX_CURRENT);
	}
	
	/**
	 * Gets the max voltage for charging
	 * 
	 * @return
	 */
	default Channel<Integer> getChargeMaxVoltage() {
		return this.channel(ChannelId.CHARGE_MAX_VOLTAGE);
	}
	
	/**
	 * Gets the max current for charging
	 * 
	 * @return
	 */
	default Channel<Integer> getChargeMaxCurrent() {
		return this.channel(ChannelId.CHARGE_MAX_CURRENT);
	}
}
