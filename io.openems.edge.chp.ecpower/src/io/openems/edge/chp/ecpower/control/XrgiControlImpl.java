package io.openems.edge.chp.ecpower.control;

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
import io.openems.edge.chp.ecpower.ro.XrgiRo;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;



@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.chp.ecpower.control", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
	EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
	EdgeEventConstants.TOPIC_CYCLE
})
public class XrgiControlImpl extends AbstractOpenemsModbusComponent implements XrgiControl, ModbusComponent, OpenemsComponent, EventHandler {

	@Reference
	private ConfigurationAdmin cm;
	
	private final Logger log = LoggerFactory.getLogger(XrgiControlImpl.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
	
	//@Reference(name = "XrgiRo", bind = "setXrgiRo", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL, unbind = "unsetXrgiRo")
	@Reference(name = "XrgiRo", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile XrgiRo xrgiRo = null;	

	private Config config = null;

	public XrgiControlImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				XrgiControl.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if(super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		
		if (!config.xrgiRo_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "XrgiRo", config.xrgiRo_id());
		}	
		
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public void applyPower(Integer activePowerTarget) {
		
		if (activePowerTarget == null) {
			return;
		}
		if (this.config.activePower() == 0) {
			return;
		}
		
		int activePowerTargetPercent = (int) Math.round(((double) activePowerTarget / this.config.activePower()) *100);
		
		/*
		 * Logic with 2 installed untis
		 * Target 0% -> 0%
		 * Target 1% -> 50% (1 unit with full load)
		 * Target 51% -> 100% (2 units with full load)
		*/
		if (config.regulationSteps() > 0) {
			int stepsActive = (activePowerTargetPercent == 0) ? 0 :
			    (int) Math.ceil(((double) activePowerTargetPercent * config.regulationSteps()) / 100);

			activePowerTargetPercent = (stepsActive * 100) / config.regulationSteps();
			
		}
		
		try {
			this._setPowerPercent(activePowerTargetPercent);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void handleEvent(Event event) {

		// super.handleEvent(event);

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			this.applyPower(19000);
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.applyPower(19000);
			break;

		}
	}	

		
		@Override
		protected ModbusProtocol defineModbusProtocol() {
		    return new ModbusProtocol(this,
				new FC16WriteRegistersTask(150, //
						this.m(XrgiControl.ChannelId.SET_POWER_PERCENT, new UnsignedWordElement(150),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1))
		        
		        // ggf. weitere Tasks für andere Bereiche
		    );
		}

/*		
		
		
		new FC3ReadRegistersTask(6, Priority.LOW, //
					m(Xrgi.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(6))
					));
		}
		*/
		
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
		return "Hello World";
	}
}
