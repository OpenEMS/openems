package io.openems.edge.battery.fenecon.f2b.cluster.serial;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.bmw.BatteryFeneconF2bBmw;
import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.battery.fenecon.f2b.cluster.common.ChannelManager;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;

public class SerialChannelManager extends ChannelManager {

	private final BatteryFeneconF2bClusterSerialImpl parent;

	private static final BiFunction<Integer, Integer, Integer> INTEGER_MIN = TypeUtils::min;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_SUM = TypeUtils::sum;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_AVG = TypeUtils::averageInt;

	public SerialChannelManager(BatteryFeneconF2bClusterSerialImpl parent) {
		super(parent);
		this.parent = parent;
	}

	@Override
	public void activate(List<BatteryFeneconF2b> batteries, BatteryFeneconF2bCluster cluster) {
		super.activate(batteries, cluster);
		this.setBatteryVoltageLimits(batteries, //
				Battery.ChannelId.CHARGE_MAX_VOLTAGE, //
				Battery.ChannelId.VOLTAGE, //
				TypeUtils::subtract, //
				TypeUtils::sum, //
				this.parent::_setChargeMaxVoltage);
		this.setBatteryVoltageLimits(batteries, //
				Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, //
				Battery.ChannelId.VOLTAGE, //
				TypeUtils::subtract, //
				TypeUtils::subtract, //
				this.parent::_setDischargeMinVoltage);
		this.calculate(INTEGER_SUM, batteries, Battery.ChannelId.VOLTAGE);
		this.calculate(INTEGER_AVG, batteries, Battery.ChannelId.CURRENT);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.CHARGE_MAX_CURRENT);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.DISCHARGE_MAX_CURRENT);
		this.updateBmwClusterVoltage(batteries);
	}

	/**
	 * Sets the cluster voltage based on the summation of link voltages before
	 * batteries started.
	 * 
	 * @param batteries the batteries
	 */
	public void updateBmwClusterVoltage(List<BatteryFeneconF2b> batteries) {
		final Consumer<Value<Boolean>> callback = (value) -> {
			final var cluster = this.parent;
			if (cluster.areAllBatteriesStopped()) {
				cluster._setInternalVoltage(null);
				return;
			}
			var batteryList = batteries.stream()//
					.filter(BatteryFeneconF2bBmw.class::isInstance) //
					.map(BatteryFeneconF2bBmw.class::cast) //
					.toList();
			Integer batteryVoltage = null;
			for (var battery : batteryList) {
				if (!battery.getInternalVoltage().isDefined()) {
					return;
				}
				batteryVoltage = TypeUtils.sum(batteryVoltage, battery.getInternalVoltage().get());
			}
			cluster._setInternalVoltage(TypeUtils.divide(batteryVoltage, 10));
		};

		for (var battery : batteries) {
			this.addOnSetNextValueListener(battery, BatteryFeneconF2b.ChannelId.INTERNAL_VOLTAGE, callback);
		}
	}
}
