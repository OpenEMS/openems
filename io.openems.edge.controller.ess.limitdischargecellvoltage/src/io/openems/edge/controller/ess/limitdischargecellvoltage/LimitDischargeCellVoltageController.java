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

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.LimitDischargeCellVoltage", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LimitDischargeCellVoltageController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LimitDischargeCellVoltageController.class);

	@Reference
	protected ComponentManager componentManager;
	
	private IState stateObject = null;

	protected LimitDischargeCellVoltageController() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				io.openems.edge.controller.ess.limitdischargecellvoltage.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// Check if configuration is ok
		if (config.chargePower() <= 0) {
			throw new OpenemsException("Charge power must be > 0");
		}
		if (config.criticalCellVoltage() > config.warningCellVoltage()) {
			throw new OpenemsException("Critical cell voltage must be lower than warning cell voltage");
		}		
		this.stateObject = createInitialStateObject(componentManager, config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {		
		log.info(getClass().getName() + ".run())");
		this.stateObject.act();
		this.stateObject = this.stateObject.getNextStateObject();
		this.channel(io.openems.edge.controller.ess.limitdischargecellvoltage.ChannelId.STATE_MACHINE).setNextValue(this.stateObject.getState());
	}

	private IState createInitialStateObject(ComponentManager manager, Config config) {
		return new Undefined(manager, config);
	}
	
//	private State handleCharge() throws OpenemsNamedException {
//		//According to the state machine the next state is normal, undefined or charge
//		ManagedSymmetricEss ess;
//		try {
//			ess = this.componentManager.getComponent(this.essId);
//		} catch (OpenemsNamedException e) {
//			log.error(e.getMessage());
//			return State.UNDEFINED;
//		}
//		
//		if (this.chargingTimeIsOver()) {
//			this.doCharging(ess, 0);
//			return State.NORMAL;
//		}
//		
//		this.doCharging(ess, this.chargePower);
//		return State.CHARGE;	
//	}
//
//	private void doCharging(ManagedSymmetricEss ess, int power) throws OpenemsNamedException {
//		ess.getSetActivePowerLessOrEquals().setNextWriteValue(power);
//	}
//
//	private boolean chargingTimeIsOver() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	private State handleCritical() {
//		//According to the state machine the next state is always charge
//		return State.CHARGE;		
//	}
//
//	private State handleWarning() {
//		// According to the state machine the next state can be undefined, normal, critical or charge
//		
//		SymmetricEss ess;
//		try {
//			ess = this.componentManager.getComponent(this.essId);
//		} catch (OpenemsNamedException e) {
//			log.error(e.getMessage());
//			return State.UNDEFINED;
//		}
//		Optional<Integer> minCellVoltageOpt = ess.getMinCellVoltage().value().asOptional();
//		if (!minCellVoltageOpt.isPresent()) {
//			return State.UNDEFINED;
//		}
//
//		int minCellVoltage = minCellVoltageOpt.get();
//		
//		if (minCellVoltage < this.criticalCellVoltage) {
//			return State.CRITICAL;
//		}
//		
//		if (minCellVoltage > this.warningCellVoltage) {
//			return State.NORMAL;	
//		}
//
//		if (this.warningTimeIsOver()) {
//			return State.CHARGE;
//		}
//
//		return State.WARNING;
//	}
//
//	private boolean warningTimeIsOver() {
//		//TODO
//		return false;
//	}
//
//	private State handleNormal() {
//		//According to the state machine the next states can be normal, warning or critical, or undefined
//		
//		SymmetricEss ess;
//		try {
//			ess = this.componentManager.getComponent(this.essId);
//		} catch (OpenemsNamedException e) {
//			log.error(e.getMessage());
//			return State.UNDEFINED;
//		}
//		Optional<Integer> minCellVoltageOpt = ess.getMinCellVoltage().value().asOptional();
//		if (!minCellVoltageOpt.isPresent()) {
//			return State.UNDEFINED;
//		}
//
//		int minCellVoltage = minCellVoltageOpt.get();
//		
//		if (minCellVoltage < this.criticalCellVoltage) {
//			return State.CRITICAL;
//		}
//		
//		if (minCellVoltage < this.warningCellVoltage) {
//			return State.WARNING;
//		}
//
//		return State.NORMAL;
//		
//	}


}
