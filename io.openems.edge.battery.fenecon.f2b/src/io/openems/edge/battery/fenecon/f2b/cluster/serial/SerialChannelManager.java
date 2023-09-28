package io.openems.edge.battery.fenecon.f2b.cluster.serial;

import java.util.List;
import java.util.function.BiFunction;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.battery.fenecon.f2b.cluster.common.ChannelManager;
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
		this.setBatteryLimits(batteries, //
				Battery.ChannelId.CHARGE_MAX_VOLTAGE, //
				Battery.ChannelId.VOLTAGE, //
				TypeUtils::subtract, //
				TypeUtils::sum, //
				this.parent::_setChargeMaxVoltage);
		this.setBatteryLimits(batteries, //
				Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, //
				Battery.ChannelId.VOLTAGE, //
				TypeUtils::subtract, //
				TypeUtils::subtract, //
				this.parent::_setDischargeMinVoltage);
		this.calculate(INTEGER_SUM, batteries, Battery.ChannelId.VOLTAGE);
		this.calculate(INTEGER_AVG, batteries, Battery.ChannelId.CURRENT);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.CHARGE_MAX_CURRENT);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.DISCHARGE_MAX_CURRENT);
	}
}
