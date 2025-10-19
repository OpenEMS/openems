package io.openems.edge.chp.ecpower.control;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;

import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.generator.api.ManagedSymmetricGenerator;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "CHP.ECcpower.control", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE })
public class XrgiControlImpl extends AbstractOpenemsModbusComponent
		implements XrgiControl, ManagedSymmetricGenerator, ModbusComponent, OpenemsComponent, EventHandler {

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final Logger log = LoggerFactory.getLogger(XrgiControlImpl.class);
	private Instant lastTransistionChangeTime = Instant.MIN;
	private int lastActiveStep = 0;

	// @Reference(name = "XrgiRo", policy = ReferencePolicy.DYNAMIC, policyOption =
	// ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	// private volatile XrgiRo xrgiRo = null;

	private Config config = null;

	public XrgiControlImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ManagedSymmetricGenerator.ChannelId.values(), ElectricityMeter.ChannelId.values(),
				XrgiControl.ChannelId.values() //
		// XrgiRo.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.config = config;
		this._setGeneratorMaxActivePower(this.config.maxActivePower());
		this._setRegulationSteps(this.config.regulationSteps());
		this.applyPreparation(false);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void applyPower(int activePowerTarget) {
		this.applyPower((Integer) activePowerTarget);
	}

	@Override
	public void applyPreparation(Boolean activate) {
		if (activate) {
			try {
			this.setChpPreparation(1);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}

		} else {
			try {
			this.setChpPreparation(0);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void applyPower(Integer activePowerTarget) {

		// should turn off CHPs
		if (activePowerTarget == null) {
			activePowerTarget = 0;
		}
		if (this.getGeneratorMaxActivePower().get() == 0) {
			return;
		}

		int stepActive = 0;
		int activePowerTargetPercent = (int) Math
				.round(((double) activePowerTarget / this.getGeneratorMaxActivePower().get()) * 100);

		activePowerTargetPercent = Math.min(100, activePowerTargetPercent); // limit to 100% to handle configuration
																			// errors

		/*
		 * Logic with 2 installed untis Target 0% -> 0% Target 1% -> 50% (1 unit with
		 * full load) Target 51% -> 100% (2 units with full load)
		 */
		if (config.regulationSteps() > 0) {
			stepActive = (activePowerTargetPercent == 0) ? 0
					: (int) Math.ceil(((double) activePowerTargetPercent * config.regulationSteps()) / 100);

			stepActive = Math.min(stepActive, config.regulationSteps()); // handle configuration errors

			if (stepActive != this.lastActiveStep) {
				if (this.isHysteresisActive(lastTransistionChangeTime, this.config.hysteresis())) {
					stepActive = this.lastActiveStep;
					this._setAwaitingHysteresis(true);
				} else {
					this.logDebug(this.log, "Transitioning from " + lastActiveStep + "->" + stepActive);

					this.lastActiveStep = stepActive;
					this.lastTransistionChangeTime = Instant.now(this.componentManager.getClock());
					this._setAwaitingHysteresis(false);

				}
			}

			activePowerTargetPercent = (stepActive * 100) / config.regulationSteps();

		}

		this._setActivePowerTarget(activePowerTarget); // feed channels
		this._setActiveRegulationStep(stepActive); // feed channels

		try {
			this._setPowerPercent(activePowerTargetPercent);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void handleEvent(Event event) {

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			// this.applyPower(19000);
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			// this.applyPower(19000);
			break;

		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, new FC16WriteRegistersTask(150, //
				this.m(XrgiControl.ChannelId.POWER_PERCENT, new UnsignedWordElement(150),
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
				this.m(XrgiControl.ChannelId.CHP_PREPARING, new UnsignedWordElement(151))

		),

				new FC3ReadRegistersTask(150, Priority.HIGH,
						this.m(XrgiControl.ChannelId.POWER_PERCENT, new UnsignedWordElement(150),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(XrgiControl.ChannelId.CHP_PREPARING, new UnsignedWordElement(151)))

		);
	}

	private boolean isHysteresisActive(Instant hysteresisTime, int configuredHysteresis) {
		if (Duration.between(//
				hysteresisTime, //
				Instant.now(this.componentManager.getClock()) //
		).toSeconds() >= configuredHysteresis) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {

		return "Regulation value: " + this.getPowerPercent().asString() + " " + "Active Regulation Step: "
				+ this.getActiveRegulationStep()

		;
	}

}
