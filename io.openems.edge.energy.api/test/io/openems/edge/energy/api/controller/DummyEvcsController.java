//package io.openems.edge.energy.api.controller;
//
//import com.google.common.collect.ImmutableSet;
//
//import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
//import io.openems.edge.common.channel.Channel;
//import io.openems.edge.common.component.AbstractOpenemsComponent;
//import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.controller.api.Controller;
//import io.openems.edge.energy.api.controller.DummyEvcsController.ScheduleMode;
//import io.openems.edge.energy.api.device.MyDummyEvcs;
//import io.openems.edge.energy.api.schedulable.Schedulable;
//import io.openems.edge.energy.api.schedulable.Schedule;
//import io.openems.edge.energy.api.schedulable.ScheduleHandler;
//import io.openems.edge.energy.api.simulatable.Simulatable;
//import io.openems.edge.energy.api.simulatable.Simulator;
//
//public class DummyEvcsController extends AbstractOpenemsComponent
//		implements Controller, Simulatable, OpenemsComponent, Schedulable<ScheduleMode> {
//
//	public enum ChargeMode {
//		FORCE_CHARGE, EXCESS_POWER, SMART;
//	}
//
//	public static class ModeConfig {
//		protected final ChargeMode chargeMode;
//
//		public ModeConfig(ChargeMode chargeMode) {
//			this.chargeMode = chargeMode;
//		}
//	}
//
//	public static enum ScheduleMode implements Schedule.ModeConfig<ScheduleMode, ModeConfig> {
//		EXCESS_POWER(new ModeConfig(ChargeMode.EXCESS_POWER)), //
//		FORCE_FAST_CHARGE(new ModeConfig(ChargeMode.FORCE_CHARGE));
//
//		private final ModeConfig modeConfig;
//
//		private ScheduleMode(ModeConfig modeConfig) {
//			this.modeConfig = modeConfig;
//		}
//
//		@Override
//		public ModeConfig getModeConfig() {
//			return this.modeConfig;
//		}
//	}
//
//	private final ScheduleHandler<ScheduleMode, ModeConfig> scheduleHandler = ScheduleHandler.of(ScheduleMode.values());
//
//	protected DummyEvcsController(String id, MyDummyEvcs evcs,
//			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
//			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
//		super(firstInitialChannelIds, furtherInitialChannelIds);
//		for (Channel<?> channel : this.channels()) {
//			channel.nextProcessImage();
//		}
//		super.activate(null, id, "", true);
//	}
//
//	public DummyEvcsController(String id, MyDummyEvcs evcs) {
//		this(id, evcs, //
//				OpenemsComponent.ChannelId.values(), //
//				Controller.ChannelId.values() //
//		);
//	}
//
//	@Override
//	public void run() throws OpenemsNamedException {
//		// ignore
//	}
//
//	@Override
//	public void applySchedule(Schedule<ScheduleMode> schedule) {
//		// ignore
//	}
//
//	@Override
//	public ImmutableSet<ScheduleMode> getAvailableModes() {
//		return this.scheduleHandler.getAvailableModes();
//	}
//
//	@Override
//	public Simulator getSimulator() {
//		return (period) -> {
//			// TODO
//		};
//	}
//}
