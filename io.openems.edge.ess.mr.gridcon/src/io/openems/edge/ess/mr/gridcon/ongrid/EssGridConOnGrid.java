package io.openems.edge.ess.mr.gridcon.ongrid;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.StateController;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "MR.Gridcon.Ongrid", //
		immediate = true, //
				configurationPolicy = ConfigurationPolicy.REQUIRE, //
				property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
				}) //
public class EssGridConOnGrid extends EssGridcon implements ManagedSymmetricEss, SymmetricEss, ModbusSlave, OpenemsComponent, EventHandler {

	@Reference
	protected ComponentManager componentManager;

	private io.openems.edge.ess.mr.gridcon.State stateObject = null;
	private Config config;

	public EssGridConOnGrid() {
		super(//
				null
//				new io.openems.edge.ess.mr.gridcon.ongrid.ChannelId[] { ChannelId.values(); }
//				//, //
////				io.openems.edge.battery.soltaro.controller.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config c) throws OpenemsNamedException {
		EssGridConOnGrid.super.activate(context, c.id(), c.alias(), c.enabled(), c.enableIPU1(), c.enableIPU2(), c.enableIPU3(), c.parameterSet(), c.gridcon_id(), c.bms_a_id(), c.bms_b_id(), c.bms_c_id());

		this.checkConfiguration(config);
		this.config = c;
		StateController.initOnGrid(componentManager, config);
		this.stateObject = StateController.getStateObject(State.UNDEFINED);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				// prepare calculated Channels
				calculateGridMode();
				calculateBatteryData();
				calculateSoc();
				calculateActivePower();

				weightingMap = getWeightingMap();
				stringControlMode = getStringControlMode();

				io.openems.edge.ess.mr.gridcon.IState nextState = this.stateObject.getNextState();
				this.stateObject = StateController.getStateObject(nextState);
				this.stateObject.act();
				this.writeChannelValues();

//				channel(ChannelId.STATE_CYCLE_ERROR).setNextValue(false);
			} catch (IllegalArgumentException | OpenemsNamedException e) {
//				channel(ChannelId.STATE_CYCLE_ERROR).setNextValue(true);
//				logError(log, "State-Cycle Error: " + e.getMessage());
			}
			break;
		}
	}
	

	private void writeChannelValues() throws OpenemsNamedException {
		this.channel(io.openems.edge.ess.mr.gridcon.ongrid.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateObject.getState());
	}

	@Override
	public String debugLog() {
		return super.debugLog() +  "State: " + stateObject.getState().getName() + "| Next State: " + stateObject.getNextState().getName();
	}

	protected void checkConfiguration(Config config) throws OpenemsException {
		// TODO  checks
	}

}
