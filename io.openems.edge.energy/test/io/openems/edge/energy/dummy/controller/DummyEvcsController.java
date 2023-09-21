package io.openems.edge.energy.dummy.controller;

import org.osgi.service.metatype.annotations.AttributeDefinition;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.energy.api.schedulable.Schedule.Handler;
import io.openems.edge.energy.api.simulatable.Simulatable;
import io.openems.edge.energy.api.simulatable.Simulator;
import io.openems.edge.energy.dummy.controller.DummyEvcsController.ScheduleHandler.DynamicConfig;
import io.openems.edge.energy.dummy.controller.DummyEvcsController.ScheduleHandler.Preset;
import io.openems.edge.energy.dummy.device.DummyEvcs;

public class DummyEvcsController extends AbstractOpenemsComponent
		implements Controller, Simulatable, OpenemsComponent, Schedulable {

	@interface Config {
		@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
		String id() default "ctrlEvcs0";

		@AttributeDefinition(name = "Charge-Mode", description = "Set the charge-mode.")
		Mode.Config chargeMode() default Mode.Config.FORCE_CHARGE;
	}

	public enum ChargeMode {
		OFF, FORCE_CHARGE, EXCESS_POWER
	}

	public class Mode {
		public static enum Config {
			FORCE_CHARGE, EXCESS_POWER, SMART;
		}
	}

	public class ScheduleHandler extends Schedule.Handler<Config, Preset, DynamicConfig> {

		public record DynamicConfig(ChargeMode chargeMode) {
		}

		public static enum Preset implements Schedule.Preset {
			OFF, //
			EXCESS_POWER, //
			FORCE_FAST_CHARGE;
		}

		protected ScheduleHandler() {
			super(Preset.values());
		}

		@Override
		protected DynamicConfig toConfig(Config config) {
			var chargeMode = switch (config.chargeMode()) {
			case FORCE_CHARGE -> ChargeMode.FORCE_CHARGE;
			case EXCESS_POWER -> ChargeMode.EXCESS_POWER;
			case SMART -> null; // Fallback
			};

			return new DynamicConfig(chargeMode);

		}

		@Override
		protected DynamicConfig toConfig(Config config, Preset preset) {
			return switch (config.chargeMode()) {
			case EXCESS_POWER, FORCE_CHARGE -> this.toConfig(config);

			case SMART -> //
				switch (preset) {
				case OFF -> new DynamicConfig(ChargeMode.OFF);
				case EXCESS_POWER -> new DynamicConfig(ChargeMode.EXCESS_POWER);
				case FORCE_FAST_CHARGE -> new DynamicConfig(ChargeMode.FORCE_CHARGE);
				};
			};
		}

	}

	private final ScheduleHandler scheduleHandler = new ScheduleHandler();

	protected DummyEvcsController(String id, DummyEvcs evcs,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyEvcsController(String id, DummyEvcs evcs) {
		this(id, evcs, //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	@Override
	public void run() throws OpenemsNamedException {
		// ignore
	}

	@Override
	public Handler<?, ?, ?> getScheduleHandler() {
		return this.scheduleHandler;
	}

	@Override
	public Simulator getSimulator() {
		return (period) -> {
			// TODO
		};
	}
}
