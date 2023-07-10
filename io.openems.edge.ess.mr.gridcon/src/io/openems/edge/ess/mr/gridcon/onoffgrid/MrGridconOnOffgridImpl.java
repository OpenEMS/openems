package io.openems.edge.ess.mr.gridcon.onoffgrid;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
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
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableCondition;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.DecisionTableConditionImpl;
import io.openems.edge.ess.mr.gridcon.state.onoffgrid.OnOffGridState;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "MR.Gridcon.OnOffgrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class MrGridconOnOffgridImpl extends EssGridcon
		implements MrGridconOnOffgrid, ManagedSymmetricEss, SymmetricEss, ModbusSlave, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Power power;

	private Config config;

	public MrGridconOnOffgridImpl() {
		super(MrGridconOnOffgrid.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config c) throws OpenemsNamedException {
		this.config = c;
		MrGridconOnOffgridImpl.super.activate(context, c.id(), c.alias(), c.enabled(), c.gridcon_id(), c.bms_a_id(),
				c.bms_b_id(), c.bms_c_id(), c.offsetCurrent());
		this.checkConfiguration(this.config);
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
		this.channel(MrGridconOnOffgrid.ChannelId.STATE_MACHINE).setNextValue(this.mainStateObject.getState());
	}

	protected void checkConfiguration(Config config) throws OpenemsException {
		// TODO checks
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected StateObject getFirstGeneralStateObjectUndefined() {
		return stateController.getGeneralStateObject(OnOffGridState.UNDEFINED);
	}

	@Override
	protected void initializeStateController(String gridconPcs, String b1, String b2, String b3) {
		DecisionTableCondition tableCondition = new DecisionTableConditionImpl(this.componentManager, gridconPcs,
				this.config.meter_id(), this.config.inputNaProtection1(), this.config.inputNaProtection2(),
				this.config.inputSyncDeviceBridge(), this.config.isNaProtection1Inverted(),
				this.config.isNaProtection2Inverted(), this.config.isInputSyncDeviceBridgeInverted());
		stateController.initDecisionTableCondition(tableCondition);
		stateController.initOnOffGrid(//
				this.componentManager, //
				gridconPcs, //
				b1, //
				b2, //
				b3, //
				this.config.enableIpu1(), //
				this.config.enableIpu2(), //
				this.config.enableIpu3(), //
				this.config.parameterSet(), //
				this.config.inputNaProtection1(), //
				this.config.isNaProtection1Inverted(), //
				this.config.inputNaProtection2(), //
				this.config.isNaProtection2Inverted(), //
				this.config.inputSyncDeviceBridge(), //
				this.config.isInputSyncDeviceBridgeInverted(), //
				this.config.outputSyncDeviceBridge(), //
				this.config.isOutputSyncDeviceBridgeInverted(), //
				this.config.outputHardReset(), //
				this.config.isOutputHardResetInverted(), //
				this.config.targetFrequencyOnGrid(), //
				this.config.targetFrequencyOffGrid(), //
				this.config.meter_id(), //
				this.config.deltaFrequency(), //
				this.config.deltaVoltage(), //
				this.config.offsetCurrent());
	}
}
