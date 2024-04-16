package io.openems.edge.deye.productionmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;

import java.util.function.Consumer;

public interface PvInverterDeye extends ElectricityMeter, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		ACTIVE_POWER_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		ACTIVE_POWER_STRING_4(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
		
		ACTIVE_POWER_GENERATOR(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),

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
		STATUS(Doc.of(Status.values())),

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
	 * @param meter the {@link ElectricityMeter}
	 * @param meter the {@link PvInverterDeye} instance being updated
	 */
	public static void calculateSumActivePowerFromPhases(PvInverterDeye meter) {
		final Consumer<Value<Integer>> calculate = ignore -> {
			// Adjust generator power if it overflows
			Integer generatorPower = meter.getActivePowerGen().getNextValue().orElse(0);
			Integer adjustedGeneratorPower = (generatorPower > 32768) ? generatorPower - 65536 : generatorPower;

			// Sum phase powers and include adjusted generator power
			Integer totalPower = TypeUtils.sum(meter.getActivePowerS1Channel().getNextValue().orElse(0),
					meter.getActivePowerS2Channel().getNextValue().orElse(0),
					meter.getActivePowerS3Channel().getNextValue().orElse(0),
					meter.getActivePowerS4Channel().getNextValue().orElse(0), adjustedGeneratorPower);

			meter._setActivePower(totalPower);
		};

		// Attach the calculation method to each relevant channel
		meter.getActivePowerS1Channel().onSetNextValue(calculate);
		meter.getActivePowerS2Channel().onSetNextValue(calculate);
		meter.getActivePowerS3Channel().onSetNextValue(calculate);
		meter.getActivePowerS4Channel().onSetNextValue(calculate);
		meter.getActivePowerGen().onSetNextValue(calculate);
	}

	public default IntegerReadChannel getActivePowerS1Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_STRING_1);
	}

	public default IntegerReadChannel getActivePowerS2Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_STRING_2);
	}

	public default IntegerReadChannel getActivePowerS3Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_STRING_3);
	}

	public default IntegerReadChannel getActivePowerS4Channel() {
		return this.channel(ChannelId.ACTIVE_POWER_STRING_4);
	}
	
	public default IntegerReadChannel getActivePowerGen() {
		return this.channel(ChannelId.ACTIVE_POWER_GENERATOR);
	}
}
