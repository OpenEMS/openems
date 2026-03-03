package io.openems.edge.deye.dccharger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.deye.enums.PLimitType;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public interface DeyeDcCharger extends EssDcCharger, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DC_POWER_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)
		.persistencePriority(PersistencePriority.HIGH)),
		DC_POWER_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)
		.persistencePriority(PersistencePriority.HIGH)),
		DC_POWER_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)
		.persistencePriority(PersistencePriority.HIGH)),
		DC_POWER_STRING_4(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)
		.persistencePriority(PersistencePriority.HIGH)),


		DC_VOLTAGE_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),		
		DC_CURRENT_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),
		
		DC_VOLTAGE_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),		
		DC_CURRENT_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),		

		DC_VOLTAGE_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),		
		DC_CURRENT_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),		

		DC_VOLTAGE_STRING_4(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),		
		DC_CURRENT_STRING_4(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),		
		
		
		
		ACTIVE_POWER_GENERATOR(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),

		ACTIVE_POWER_STRING_4(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),		
		
		
		LAST_UPDATE_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS)), //
		PDC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		UDC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		YESTERDAY_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		MONTHLY_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		YEARLY_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.CUMULATED_WATT_HOURS)),
		PAC_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		YESTERDAY_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		MONTHLY_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		YEARLY_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.CUMULATED_WATT_HOURS)),
		TOTAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS_BY_WATT_PEAK)),

		// PV
		P_LIMIT_TYPE(Doc.of(PLimitType.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		P_LIMIT_PERC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.PERCENT)),
		P_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT)),
		WATCH_DOG_TAG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
				.text("PV-Limit failed"));

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
	 * Updates the total active power of the meter by summing the individual phase
	 * powers and adjusting for generator power. This method assumes all necessary
	 * values are always present and uses a simple fallback for any missing data.
	 *
	 * @param meter the {@link DeyeDcCharger} instance being updated
	 
	public static void calculateSumActivePowerFromPhases(DeyeDcCharger meter) {
		final Consumer<Value<Integer>> calculate = ignore -> {
			// Adjust generator power if it overflows
			Integer generatorPower = meter.getActivePowerGen().getNextValue().orElse(0);
			Integer adjustedGeneratorPower = (generatorPower > 32768) ? generatorPower - 65536 : generatorPower;

			// Sum phase powers and include adjusted generator power
			Integer totalPower = TypeUtils.sum(meter.getActivePowerS1Channel().getNextValue().orElse(0),
					meter.getActivePowerS2Channel().getNextValue().orElse(0),
					meter.getActivePowerS3Channel().getNextValue().orElse(0),
					meter.getActivePowerS4Channel().getNextValue().orElse(0), adjustedGeneratorPower);

			//meter._setActivePower(totalPower);
		};

		// Attach the calculation method to each relevant channel
		meter.getActivePowerS1Channel().onSetNextValue(calculate);
		meter.getActivePowerS2Channel().onSetNextValue(calculate);
		meter.getActivePowerS3Channel().onSetNextValue(calculate);
		meter.getActivePowerS4Channel().onSetNextValue(calculate);
		meter.getActivePowerGen().onSetNextValue(calculate);
	}
	*/



	/**
	 * Gets the Channel for {@link ChannelId#DC_POWER_STRING_1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerString1Channel() {
		return this.channel(ChannelId.DC_POWER_STRING_1);
	}


	public default Value<Integer> getDcPowerString1() {
		return this.getDcPowerString1Channel().value();
	}


	public default void _setDcPowerString1(Integer value) {
		this.getDcPowerString1Channel().setNextValue(value);
	}


	public default void _setDcPowerString1(int value) {
		this.getDcPowerString1Channel().setNextValue(value);
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#DC_POWER_STRING_1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerString2Channel() {
		return this.channel(ChannelId.DC_POWER_STRING_2);
	}


	public default Value<Integer> getDcPowerString2() {
		return this.getDcPowerString2Channel().value();
	}


	public default void _setDcPowerString2(Integer value) {
		this.getDcPowerString2Channel().setNextValue(value);
	}


	public default void _setDcPowerString2(int value) {
		this.getDcPowerString2Channel().setNextValue(value);
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#DC_POWER_STRING_3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerString3Channel() {
		return this.channel(ChannelId.DC_POWER_STRING_3);
	}


	public default Value<Integer> getDcPowerString3() {
		return this.getDcPowerString3Channel().value();
	}


	public default void _setDcPowerString3(Integer value) {
		this.getDcPowerString3Channel().setNextValue(value);
	}


	public default void _setDcPowerString3(int value) {
		this.getDcPowerString3Channel().setNextValue(value);
	}	
		
	/**
	 * Gets the Channel for {@link ChannelId#DC_POWER_STRING_4}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerString4Channel() {
		return this.channel(ChannelId.DC_POWER_STRING_4);
	}


	public default Value<Integer> getDcPowerString4() {
		return this.getDcPowerString4Channel().value();
	}


	public default void _setDcPowerString4(Integer value) {
		this.getDcPowerString4Channel().setNextValue(value);
	}


	public default void _setDcPowerString4(int value) {
		this.getDcPowerString4Channel().setNextValue(value);
	}


	boolean hasError();


	boolean hasWarning();	
		
	
	
	
	
}
