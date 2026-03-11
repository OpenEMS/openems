package io.openems.edge.pytes.battery;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1_AND_INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.pytes.ess.PytesJs3;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Pytes.Battery", //
		immediate = true, //
		configurationPolicy = REQUIRE //
) //
public class PytesBatteryImpl extends AbstractOpenemsModbusComponent
		implements Battery, PytesBattery, ModbusComponent, OpenemsComponent, ModbusSlave {

	public static final int DEFAULT_UNIT_ID = 225;
	public static final int BATTERY_VOLTAGE = 48;

	private int minSocPercentage;

	@Reference
	protected ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(PytesBatteryImpl.class);

	protected Config config;

	public PytesBatteryImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				PytesBattery.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}
	
	@Reference(
		    name = "ess",
		    policy = ReferencePolicy.STATIC,
		    policyOption = ReferencePolicyOption.GREEDY,
		    cardinality = ReferenceCardinality.MANDATORY
		)
		private volatile PytesJs3 ess;
	


	private int maxSocPercentage;

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;

	    if (super.activate(context, config.id(), config.alias(), config.enabled(),
	            this.ess.getUnitId(), this.cm, "Modbus",
	            this.ess.getModbusBridgeId())) {
	        return;
	    }	   
	    
	    if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(),
	            "ess", config.ess_id())) {
	        return;
	    }	    
	    
	    this.ess.addBattery(this);



		this.installListener();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "SoC: " + this.getSoc()
		+ " DcPower: " + this.getDcDischargePower().asString();
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
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// TODO implement battery start/stop if needed
		this._setStartStop(value);
	}

	private void checkSocControllers() {

		this.maxSocPercentage = 100; // Default max SoC

		this.logDebug(this.log, "checkSocControllers: MinSoC set to " + this.minSocPercentage + ", MaxSoC set to "
				+ this.maxSocPercentage);
	}

	private void installListener() {
		
		
	}

	@Override
	public void setMinSocPercentage(int minSocPercentage) {
		this.minSocPercentage = minSocPercentage;

	}
	

	@Override
	public int getConfiguredMaxChargeCurrent() {
		return this.config.max_current(); // ToDo: different values for charge/discharge useful?
	}

	@Override
	public int getConfiguredMaxDischargeCurrent() {
		return this.config.max_current();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				
				new FC4ReadInputRegistersTask(33139, Priority.HIGH, //
						
						m(Battery.ChannelId.SOC, new UnsignedWordElement(33139)),
								
						m(Battery.ChannelId.SOH, new UnsignedWordElement(33140)),
								
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(33141),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
								
						m(Battery.ChannelId.CURRENT, new SignedWordElement(33142),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
								
						m(PytesBattery.ChannelId.BMS_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(33143),
								ElementToChannelConverter.SCALE_FACTOR_2),
								
						m(PytesBattery.ChannelId.BMS_DISCHARGE_CURRENT_LIMIT, new UnsignedWordElement(33144),
								ElementToChannelConverter.SCALE_FACTOR_2),
								
						m(PytesBattery.ChannelId.BMS_BATTERY_FAULT_STATUS01, new UnsignedWordElement(33145),
								ElementToChannelConverter.SCALE_FACTOR_2),
								
						m(PytesBattery.ChannelId.BMS_BATTERY_FAULT_STATUS02, new UnsignedWordElement(33146),
								ElementToChannelConverter.SCALE_FACTOR_2),
						
						new DummyRegisterElement(33147, 33148), // Reserved						
						
						m(PytesBattery.ChannelId.DC_DISCHARGE_POWER, new SignedDoublewordElement(33149))								
				
				));

	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Battery.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(PytesBattery.class, accessMode, 100) //
						//.channel(0, PytesBattery.ChannelId.CHARGE_CYCLES, ModbusType.UINT16) //
						//.channel(1, PytesBattery.ChannelId.DC_CHARGED_ENERGY, ModbusType.UINT16) //
						//.channel(2, PytesBattery.ChannelId.DC_DISCHARGED_ENERGY, ModbusType.UINT16) //

						.build());
	}
}
