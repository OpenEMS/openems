package io.openems.edge.battery.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Battery extends OpenemsComponent {

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
		 * State of Health
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
		SOH(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT)),
		/**
		 * Max capacity
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		MAX_CAPACITY(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT_HOURS)),
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
		/**
		 * Battery Temperature
		 *
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * <li>Unit: Celsius
		 * <li>Range: (-50)..100
		 * </ul>
		 */
		BATTERY_TEMP(new Doc().type(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)),
		/**
		 * Indicates that the battery has started and is ready for charging/discharging
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Boolean
		 * </ul>
		 */
		READY_FOR_WORKING(new Doc().type(OpenemsType.BOOLEAN)),
		
		/**
		 * Capacity of battery
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		CAPACITY_KWH(new Doc().type(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
		
		/**
		 * Voltage of battery
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		VOLTAGE(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT)),
		
		/**
		 * Minimal cell voltage
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		MINIMAL_CELL_VOLTAGE(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),
		
		/**
		 * Maximal power
		 * 
		 * <ul>
		 * <li>Interface: Battery
		 * <li>Type: Integer
		 * </ul>
		 */
		MAXIMAL_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)),
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
	 * Gets the State of Health in [%], range 0..100 %
	 *
	 * @return
	 */
	default Channel<Integer> getSoh() {
		return this.channel(ChannelId.SOH);
	}

	/**
	 * Gets the maximum capacity
	 *
	 * @return
	 */
	default Channel<Integer> getMaxCapacity() {
		return this.channel(ChannelId.MAX_CAPACITY);
	}

	/**
	 * Gets the Battery Temperature in [degC], range (-50)..100
	 *
	 * @return
	 */
	default Channel<Integer> getBatteryTemp() {
		return this.channel(ChannelId.BATTERY_TEMP);
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
	
	/**
	 * Gets the indicator if ready to charge/discharge
	 * 
	 * @return
	 */
	default Channel<Boolean> getReadyForWorking() {
		return this.channel(ChannelId.READY_FOR_WORKING);
	}
	
	/**
	 * Gets the capacity of this battery
	 * 
	 * @return
	 */
	default Channel<Integer> getCapacity() {
		return this.channel(ChannelId.CAPACITY_KWH);
	}
	
	/**
	 * Gets the total voltage of this battery system
	 * 
	 * @return
	 */
	default Channel<Integer> getVoltage() {
		return this.channel(ChannelId.VOLTAGE);
	}
	
	/**
	 * Gets the minimal cell voltage of this battery
	 * 
	 * @return
	 */
	default Channel<Integer> getMinimalCellVoltage() {
		return this.channel(ChannelId.MINIMAL_CELL_VOLTAGE);
	}

	/**
	 * Gets the maximal power
	 * 
	 * @return
	 */
	default Channel<Integer> getMaxPower() {
		return this.channel(ChannelId.MAXIMAL_POWER);
	}
}
