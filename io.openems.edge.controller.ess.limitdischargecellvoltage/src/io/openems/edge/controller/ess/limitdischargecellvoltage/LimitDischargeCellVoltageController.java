package io.openems.edge.controller.ess.limitdischargecellvoltage;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.limitdischargecellvoltage.state.Undefined;
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.LimitDischargeCellVoltage", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LimitDischargeCellVoltageController extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LimitDischargeCellVoltageController.class);

	@Reference
	protected ComponentManager componentManager;

	private IState stateObject = null;
	private Config config;

	public LimitDischargeCellVoltageController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				io.openems.edge.controller.ess.limitdischargecellvoltage.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		checkConfiguration(config);
		this.config = config;
		this.stateObject = createInitialStateObject(componentManager, config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		log.info("run())");
		this.stateObject.act();
		this.stateObject = this.stateObject.getNextStateObject();
		this.writeChannelValues();
	}

	private void writeChannelValues() throws OpenemsNamedException {
		this.channel(io.openems.edge.controller.ess.limitdischargecellvoltage.ChannelId.STATE_MACHINE)
				.setNextValue(this.stateObject.getState());

		SymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		this.channel(io.openems.edge.controller.ess.limitdischargecellvoltage.ChannelId.MIN_CELL_VOLTAGE)
				.setNextValue(ess.getMinCellVoltage().value());
		this.channel(io.openems.edge.controller.ess.limitdischargecellvoltage.ChannelId.ESS_POWER)
				.setNextValue(ess.getActivePower().value());
	}

	private IState createInitialStateObject(ComponentManager manager, Config config) {
		return new Undefined(manager, config);
	}

	private void checkConfiguration(Config config) throws OpenemsException {
		if (config.chargePowerPercent() <= 0 || config.chargePowerPercent() > 100) {
			throw new OpenemsException("Charge power percentage must be > 0 and < 100");
		}
		if (config.criticalCellVoltage() >= config.warningCellVoltage()) {
			throw new OpenemsException("Critical cell voltage must be lower than warning cell voltage");
		}
	}
}
