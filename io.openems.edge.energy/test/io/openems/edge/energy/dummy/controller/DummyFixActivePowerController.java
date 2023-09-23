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
import io.openems.edge.energy.api.simulatable.PresetSimulator;
import io.openems.edge.energy.dummy.controller.DummyFixActivePowerController.ScheduleHandler.DynamicConfig;
import io.openems.edge.energy.dummy.controller.DummyFixActivePowerController.ScheduleHandler.Preset;
import io.openems.edge.energy.dummy.device.DummyEss;

public class DummyFixActivePowerController extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, Schedulable {

	private @interface Config {
		@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
		String id() default "ctrlEvcs0";

		@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
		Mode.Config mode() default Mode.Config.MANUAL_ON;

		@AttributeDefinition(name = "Charge/Discharge power [W]", description = "Negative values for Charge; positive for Discharge")
		int power();
	}

	public enum Mode {

		MANUAL_ON, MANUAL_OFF;

		public static enum Config {
			MANUAL_ON, MANUAL_OFF, SMART;
		}
	}

	public class ScheduleHandler extends Schedule.Handler<Config, Preset, DynamicConfig> {

		public record DynamicConfig(Mode mode, int power) {
		}

		public static enum Preset implements Schedule.Preset {
			OFF, //
			FORCE_ZERO, //
			FORCE_DISCHARGE_5000, //
			FORCE_CHARGE_5000;
		}

		protected ScheduleHandler() {
			super(Preset.values());
		}

		@Override
		protected DynamicConfig toConfig(Config config) {
			var mode = switch (config.mode()) {
			case MANUAL_ON -> Mode.MANUAL_ON;
			case MANUAL_OFF -> Mode.MANUAL_OFF;
			case SMART -> null; // Fallback
			};

			return new DynamicConfig(mode, config.power());
		}

		@Override
		protected DynamicConfig toConfig(Config config, Preset preset) {
			return switch (config.mode()) {
			case MANUAL_ON, MANUAL_OFF -> this.toConfig(config);

			case SMART -> //
				switch (preset) {
				case OFF -> new DynamicConfig(Mode.MANUAL_OFF, 0);
				case FORCE_CHARGE_5000 -> new DynamicConfig(Mode.MANUAL_ON, -5000);
				case FORCE_ZERO -> new DynamicConfig(Mode.MANUAL_ON, 0);
				case FORCE_DISCHARGE_5000 -> new DynamicConfig(Mode.MANUAL_ON, 5000);
				};
			};
		}

		@Override
		protected PresetSimulator generateSimulator() {
//			return (period) -> {
			// return (period, componentId) -> {
			// var mode = period.<ScheduleMode>getMode(componentId);
			// var power = mode.getConfig().power;
			// if (power != null) {
			// period.setStorage(DummyFixActivePowerController.this.id(), power);
			// }
			// };
			// TODO
//			};
			return null;
		}
	}

	private final ScheduleHandler scheduleHandler = new ScheduleHandler();

	protected DummyFixActivePowerController(String id, DummyEss ess,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	public DummyFixActivePowerController(String id, DummyEss ess) {
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
	public Handler<?, ?, ?> getScheduleHandler() {
		return this.scheduleHandler;
	}
}
