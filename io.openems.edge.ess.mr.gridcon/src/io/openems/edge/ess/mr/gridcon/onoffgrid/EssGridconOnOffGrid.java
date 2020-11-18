package io.openems.edge.ess.mr.gridcon.onoffgrid;

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
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableConditionImpl;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "MR.Gridcon.OnOffgrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		})
public class EssGridconOnOffGrid extends EssGridcon
		implements ManagedSymmetricEss, SymmetricEss, ModbusSlave, OpenemsComponent, EventHandler {

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private Power power;
	private Config config;

	public EssGridconOnOffGrid() {
		super(io.openems.edge.ess.mr.gridcon.onoffgrid.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config c) throws OpenemsNamedException {
		this.config = c;
		EssGridconOnOffGrid.super.activate(context, c.id(), c.alias(), c.enabled(), c.gridcon_id(), c.bms_a_id(),
				c.bms_b_id(), c.bms_c_id(), c.offsetCurrent());
		this.checkConfiguration(config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	protected void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException {
		// TODO
		GridMode gridMode = GridMode.UNDEFINED;

		if (this.mainStateObject != null) {
			if (//
			OnOffGridState.ON_GRID_MODE == this.mainStateObject.getState()//
			) {
				gridMode = GridMode.ON_GRID;
			} else if (OnOffGridState.OFF_GRID_MODE == this.mainStateObject.getState()) {
				gridMode = GridMode.OFF_GRID;
			}
		}

		_setGridMode(gridMode);
	}

	@Override
	protected void writeStateMachineToChannel() {
		this.channel(io.openems.edge.ess.mr.gridcon.onoffgrid.ChannelId.STATE_MACHINE)
				.setNextValue(this.mainStateObject.getState());
	}

	protected void checkConfiguration(Config config) throws OpenemsException {
		// TODO checks
	}

	@Override
	public Power getPower() {
		return power;
	}

	@Override
	protected ComponentManager getComponentManager() {
		return componentManager;
	}

	@Override
	protected io.openems.edge.ess.mr.gridcon.StateObject getFirstGeneralStateObjectUndefined() {
		return stateController.getGeneralStateObject(OnOffGridState.UNDEFINED);
	}

	@Override
	protected void initializeStateController(String gridconPcs, String b1, String b2, String b3) {
		DecisionTableCondition tableCondition = new DecisionTableConditionImpl(componentManager, gridconPcs,
				config.meter_id(), config.inputNaProtection1(), config.inputNaProtection2(),
				config.inputSyncDeviceBridge(), config.isNaProtection1Inverted(), config.isNaProtection2Inverted(),
				config.isInputSyncDeviceBridgeInverted());
		stateController.initDecisionTableCondition(tableCondition);
		stateController.initOnOffGrid(//
				componentManager, //
				gridconPcs, //
				b1, //
				b2, //
				b3, //
				config.enableIpu1(), //
				config.enableIpu2(), //
				config.enableIpu3(), //
				config.parameterSet(), //
				config.inputNaProtection1(), //
				config.isNaProtection1Inverted(), //
				config.inputNaProtection2(), //
				config.isNaProtection2Inverted(), //
				config.inputSyncDeviceBridge(), //
				config.isInputSyncDeviceBridgeInverted(), //
				config.outputSyncDeviceBridge(), //
				config.isOutputSyncDeviceBridgeInverted(), //
				config.outputHardReset(), //
				config.isOutputHardResetInverted(), //
				config.targetFrequencyOnGrid(), //
				config.targetFrequencyOffGrid(), //
				config.meter_id(), //
				config.deltaFrequency(), //
				config.deltaVoltage(), //
				config.offsetCurrent());
	}
}
