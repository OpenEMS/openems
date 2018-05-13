package io.openems.edge.meter.asymmetric.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.converter.StaticConverters;
import io.openems.edge.meter.api.Meter;

/**
 * Represents an Asymmetric Meter.
 * 
 * - Negative ActivePowerL1/L2/L3 and ConsumptionActivePowerL1/L2/L3 represent
 * Consumption, i.e. power that is 'leaving the system', e.g. feed-to-grid
 * 
 * - Positive ActivePowerL1/L2/L3 and ProductionActivePowerL1/L2/L3 represent
 * Production, i.e. power that is 'entering the system', e.g. buy-from-grid
 * 
 */
public interface AsymmetricMeter extends Meter {

	public final static String POWER_DOC_TEXT = "Negative values for Consumption; positive for Production";

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Consumption Active Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * ACTIVE_POWER_L1
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Consumption Active Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * ACTIVE_POWER_L2
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Consumption Active Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * ACTIVE_POWER_L3
		 * </ul>
		 */
		CONSUMPTION_ACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Production Active Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER_L1
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Production Active Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER_L2
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Production Active Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER_L3
		 * </ul>
		 */
		PRODUCTION_ACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Active Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER_L1(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.PRODUCTION_ACTIVE_POWER_L1)
								.setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L1).setNextValue(chargeValue);
					});
				})), //
		/**
		 * Active Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER_L2(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.PRODUCTION_ACTIVE_POWER_L2)
								.setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L2).setNextValue(chargeValue);
					});
				})), //
		/**
		 * Active Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		ACTIVE_POWER_L3(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.PRODUCTION_ACTIVE_POWER_L3)
								.setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L3).setNextValue(chargeValue);
					});
				})), //
		/**
		 * Consumption Reactive Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * REACTIVE_POWER_L1
		 * </ul>
		 */
		CONSUMPTION_REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Consumption Reactive Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * REACTIVE_POWER_L2
		 * </ul>
		 */
		CONSUMPTION_REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Consumption Reactive Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * REACTIVE_POWER_L3
		 * </ul>
		 */
		CONSUMPTION_REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from
		 * REACTIVE_POWER_L1
		 * </ul>
		 */
		PRODUCTION_REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from
		 * REACTIVE_POWER_L2
		 * </ul>
		 */
		PRODUCTION_REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Production Reactive Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from
		 * REACTIVE_POWER_L3
		 * </ul>
		 */
		PRODUCTION_REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Reactive Power L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L1(new Doc().type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.PRODUCTION_REACTIVE_POWER_L1)
								.setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L1)
								.setNextValue(chargeValue);
					});
				})),
		/**
		 * Reactive Power L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L2(new Doc().type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.PRODUCTION_REACTIVE_POWER_L2)
								.setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L2)
								.setNextValue(chargeValue);
					});
				})),
		/**
		 * Reactive Power L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Consumption (power that is 'leaving the
		 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
		 * the system')
		 * </ul>
		 */
		REACTIVE_POWER_L3(new Doc().type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.PRODUCTION_REACTIVE_POWER_L3)
								.setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L3)
								.setNextValue(chargeValue);
					});
				})),
		/**
		 * Voltage L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		/**
		 * Voltage L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		/**
		 * Voltage L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * </ul>
		 */
		VOLTAGE_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		/**
		 * Current L1
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L1(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		/**
		 * Current L2
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L2(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		/**
		 * Current L3
		 * 
		 * <ul>
		 * <li>Interface: Meter Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_L3(new Doc().type(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Active Power for L1 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL1() {
		return this.channel(ChannelId.ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Active Power for L2 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL2() {
		return this.channel(ChannelId.ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Active Power for L3 in [W]. Negative values for Consumption;
	 * positive for Production
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL3() {
		return this.channel(ChannelId.ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Consumption Active Power for L1 in [W]. This is derived from
	 * negative 'getActivePowerL1()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePowerL1() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Consumption Active Power for L2 in [W]. This is derived from
	 * negative 'getActivePowerL2()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePowerL2() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Consumption Active Power for L3 in [W]. This is derived from
	 * negative 'getActivePowerL3()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionActivePowerL3() {
		return this.channel(ChannelId.CONSUMPTION_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Production Active Power for L1 in [W]. This is derived from positive
	 * 'getActivePowerL1()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePowerL1() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Production Active Power for L2 in [W]. This is derived from positive
	 * 'getActivePowerL2()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePowerL2() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Production Active Power for L3 in [W]. This is derived from positive
	 * 'getActivePowerL3()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionActivePowerL3() {
		return this.channel(ChannelId.PRODUCTION_ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Reactive Power for L1 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL1() {
		return this.channel(ChannelId.REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Reactive Power for L2 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL2() {
		return this.channel(ChannelId.REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Reactive Power for L3 in [var]. Negative values for Consumption;
	 * positive for Production.
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL3() {
		return this.channel(ChannelId.REACTIVE_POWER_L3);
	}

	/**
	 * Gets the Consumption Reactive Power for L1 in [var]. This is derived from
	 * negative 'getReactivePowerL2()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePowerL1() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Consumption Reactive Power for L2 in [var]. This is derived from
	 * negative 'getReactivePowerL2()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePowerL2() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Consumption Reactive Power for L3 in [var]. This is derived from
	 * negative 'getReactivePowerL3()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getConsumptionReactivePowerL3() {
		return this.channel(ChannelId.CONSUMPTION_REACTIVE_POWER_L3);
	}

	/**
	 * Gets the Production Reactive Power for L1 in [var]. This is derived from
	 * positive 'getReactivePowerL1()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePowerL1() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Production Reactive Power for L2 in [var]. This is derived from
	 * positive 'getReactivePowerL2()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePowerL2() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Production Reactive Power for L3 in [var]. This is derived from
	 * positive 'getReactivePowerL3()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getProductionReactivePowerL3() {
		return this.channel(ChannelId.PRODUCTION_REACTIVE_POWER_L3);
	}

}
