package io.openems.edge.energy.api.controller;

import com.google.common.collect.ImmutableSet;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.controller.DummyFixActivePowerController.ScheduleMode;
import io.openems.edge.energy.api.device.MyDummyEss;
import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.energy.api.schedulable.ScheduleHandler;
import io.openems.edge.energy.api.simulatable.ModeSimulator;
import io.openems.edge.energy.api.simulatable.Simulatable;

public class DummyFixActivePowerController extends AbstractOpenemsComponent
		implements Controller, Simulatable, OpenemsComponent, Schedulable<ScheduleMode> {

	public static class ModeConfig {
		protected final Integer power;

		public ModeConfig(Integer power) {
			this.power = power;
		}
	}

	// TODO percentage
	// TODO limit charge mode -> avoid force_charge instead of feed-in
	public static enum ScheduleMode implements Schedule.ModeConfig<ScheduleMode, ModeConfig> {
		/* OFF */BALANCING(new ModeConfig(null)), //
		FORCE_ZERO(new ModeConfig(0)), //
		FORCE_CHARGE_2000(new ModeConfig(-2000)), //
		FORCE_CHARGE_5000(new ModeConfig(-5000)); //

		private final ModeConfig modeConfig;

		private ScheduleMode(ModeConfig modeConfig) {
			this.modeConfig = modeConfig;
		}

		@Override
		public ModeConfig getModeConfig() {
			return this.modeConfig;
		}
	}

	private final ScheduleHandler<ScheduleMode, ModeConfig> scheduleHandler = ScheduleHandler.of(ScheduleMode.values());

	protected DummyFixActivePowerController(String id, MyDummyEss ess,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyFixActivePowerController(String id, MyDummyEss ess) {
		this(id, ess, //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	@Override
	public void run() throws OpenemsNamedException {
		// ignore
	}

	@Override
	public void applySchedule(Schedule<ScheduleMode> schedule) {
		// ignore
	}

	@Override
	public ImmutableSet<ScheduleMode> getAvailableModes() {
		return this.scheduleHandler.getAvailableModes();
	}

	@Override
	public ModeSimulator<ScheduleMode> getSimulator() {
		return (period, componentId) -> {
			var mode = period.<ScheduleMode>getMode(componentId);
			var power = mode.getModeConfig().power;
			if (power != null) {
				period.setStorage(DummyFixActivePowerController.this.id(), power);
			}
		};
	}
}
