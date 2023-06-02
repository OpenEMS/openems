package io.openems.edge.ess.mr.gridcon.ongrid;

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
import io.openems.edge.ess.mr.gridcon.state.ongrid.OnGridState;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "MR.Gridcon.Ongrid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class EssGridconOnGridImpl extends EssGridcon
		implements EssGridconOnGrid, ManagedSymmetricEss, SymmetricEss, ModbusSlave, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Power power;

	private Config config;

	public EssGridconOnGridImpl() {
		super(EssGridconOnGrid.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config c) throws OpenemsNamedException {
		this.config = c;
		EssGridconOnGridImpl.super.activate(context, c.id(), c.alias(), c.enabled(), c.gridcon_id(), c.bms_a_id(),
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
		GridMode gridMode = GridMode.ON_GRID;
		_setGridMode(gridMode);
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
	protected io.openems.edge.ess.mr.gridcon.StateObject getFirstGeneralStateObjectUndefined() {
		return stateController.getGeneralStateObject(OnGridState.UNDEFINED);
	}

	@Override
	protected void initializeStateController(String gridconPcs, String b1, String b2, String b3) {
		this.stateController.initOnGrid(this.componentManager, gridconPcs, b1, b2, b3, this.config.enableIpu1(),
				this.config.enableIpu2(), this.config.enableIpu3(),
				/* config.parameterSet(), */ this.config.outputHardReset(), this.config.offsetCurrent());
	}

	@Override
	protected void writeStateMachineToChannel() {
		this.channel(EssGridconOnGrid.ChannelId.STATE_MACHINE).setNextValue(this.mainStateObject.getState());
	}
}
