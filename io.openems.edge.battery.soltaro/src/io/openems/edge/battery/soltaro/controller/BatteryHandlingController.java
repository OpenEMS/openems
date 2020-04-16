package io.openems.edge.battery.soltaro.controller;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.soltaro.controller.state.StateController;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Soltaro.Batteryhandling", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BatteryHandlingController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	@Reference
	protected ComponentManager componentManager;

	private IState stateObject = null;
	private Config config;

	public BatteryHandlingController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				io.openems.edge.battery.soltaro.controller.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.checkConfiguration(config);
		this.config = config;
		StateController.init(componentManager, config);
		this.stateObject = StateController.getStateObject(State.UNDEFINED);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		State nextState = this.stateObject.getNextState();
		this.stateObject = StateController.getStateObject(nextState);
		this.stateObject.act();
		this.writeChannelValues();
	}

	private void writeChannelValues() throws OpenemsNamedException {
		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateObject.getState());

		SymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.MIN_CELL_VOLTAGE)
				.setNextValue(ess.getMinCellVoltage().value());
		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.MAX_CELL_VOLTAGE)
				.setNextValue(ess.getMaxCellVoltage().value());
		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.MIN_CELL_TEMPERATURE)
				.setNextValue(ess.getMinCellTemperature().value());
		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.MAX_CELL_TEMPERATURE)
				.setNextValue(ess.getMaxCellTemperature().value());
		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.ESS_POWER)
				.setNextValue(ess.getActivePower().value());
		this.channel(io.openems.edge.battery.soltaro.controller.ChannelId.ESS_SOC) //
				.setNextValue(ess.getSoc().value());
	}

	@Override
	public String debugLog() {
		State currentState = stateObject.getState();
		State nextState = stateObject.getNextState();
		if (currentState == nextState) {
			return "State:" + currentState.getName();
		} else {
			return "Changing State from [" + currentState.getName() + "] to [" + nextState.getName() + "]";
		}
	}

	protected void checkConfiguration(Config config) throws OpenemsException {
		if (config.chargePowerPercent() <= 0 || config.chargePowerPercent() > 100) {
			throw new OpenemsException("ForceCharge power percentage must be > 0 and < 100");
		}
		if (config.criticalLowCellVoltage() >= config.warningLowCellVoltage()) {
			throw new OpenemsException("Critical low cell voltage must be lower than warning low cell voltage");
		}
		// TODO finish checks
	}
}
