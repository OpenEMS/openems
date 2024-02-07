package io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine;

import java.time.Clock;
import java.util.List;

import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.system.fenecon.industrial.s.SystemFeneconIndustrialS;

public class CoolingUnitContext extends AbstractContext<SystemFeneconIndustrialS> {

	protected final Clock clock;
	protected final List<BatteryFeneconF2b> batteries;
	protected final Channel<Boolean> inputCoolingUnitErrorChannel;
	protected final WriteChannel<Boolean> outputCoolingUnitEnableChannel;

	public CoolingUnitContext(SystemFeneconIndustrialS parent, //
			Clock clock, //
			List<BatteryFeneconF2b> batteries, //
			Channel<Boolean> inputCoolingUnitErrorChannel, //
			WriteChannel<Boolean> outputCoolinUnitEnableChannel) {
		super(parent);
		this.clock = clock;
		this.batteries = batteries;
		this.inputCoolingUnitErrorChannel = inputCoolingUnitErrorChannel;
		this.outputCoolingUnitEnableChannel = outputCoolinUnitEnableChannel;
	}
}
