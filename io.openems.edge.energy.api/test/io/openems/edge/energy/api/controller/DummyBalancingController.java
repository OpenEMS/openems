//package io.openems.edge.energy.api.controller;
//
//import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
//import io.openems.edge.common.channel.Channel;
//import io.openems.edge.common.component.AbstractOpenemsComponent;
//import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.controller.api.Controller;
//import io.openems.edge.energy.api.device.MyDummyEss;
//import io.openems.edge.energy.api.simulatable.Simulatable;
//import io.openems.edge.energy.api.simulatable.Simulator;
//
//public class DummyBalancingController extends AbstractOpenemsComponent
//		implements Controller, Simulatable, OpenemsComponent {
//
//	private final MyDummyEss ess;
//
//	protected DummyBalancingController(String id, MyDummyEss ess,
//			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
//			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
//		super(firstInitialChannelIds, furtherInitialChannelIds);
//		for (Channel<?> channel : this.channels()) {
//			channel.nextProcessImage();
//		}
//		super.activate(null, id, "", true);
//		this.ess = ess;
//	}
//
//	public DummyBalancingController(String id, MyDummyEss ess) {
//		this(id, ess, //
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
//	public Simulator getSimulator() {
//		return (period) -> {
//			if (period.getStorage() != null) {
//				return; // has already been set
//				// TODO should use linear equation system (EssPower) or interval instead
//			}
//
//			period.setStorage(DummyBalancingController.this.ess.id(),
//					period.forecast.consumption - period.forecast.production);
//		};
//	}
//}
