package io.openems.edge.thermometer.esera.onewire;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.esera.onewire.enums.OwdStatus;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Thermometer.Esera.OneWire", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class EseraOneWireThermometerImpl extends AbstractOpenemsModbusComponent
		implements Thermometer, EseraOneWireThermometer, ModbusComponent, EventHandler, OpenemsComponent {


	@Reference
	private ConfigurationAdmin cm;
	private Config config;
	private final Logger log = LoggerFactory.getLogger(EseraOneWireThermometerImpl.class);
	
	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EseraOneWireThermometerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EseraOneWireThermometer.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.addModbusTask(this.getModbusProtocol());
		
		
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
	
	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "\n" + this.config.alias() + " Debug/Temp " + this.getTemperatureOwdDebug().asString() + "/" + this.getTemperatureOwd() + " \n"
			;

		} else {
			return "\n" + this.config.alias() + " Temp " + this.getTemperatureOwd() + " \n" 
					;
		}

	}


	private void addModbusTask(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
				new FC3ReadRegistersTask(this.config.OneWireDevice().getModbusAddress(), Priority.HIGH,
						m(EseraOneWireThermometer.ChannelId.TEMPERATURE_OWD_DEBUG, new SignedWordElement(this.config.OneWireDevice().getModbusAddress())),
						new DummyRegisterElement(this.config.OneWireDevice().getModbusAddress()+1, this.config.OneWireDevice().getModbusAddress() +11),
						m(EseraOneWireThermometer.ChannelId.OWD_STATUS, new SignedDoublewordElement(this.config.OneWireDevice().getModbusAddress()+12))));
	}

	public void handleEvent(Event event) {
		// super.handleEvent(event);

		if (event.getTopic() == EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS) {
			this.validateAndSetTemperatures();

		}

	}

	private void validateAndSetTemperatures() {

		if (!this.getOwdStatus().equals(OwdStatus.NORMAL)) {
			this._setOwdReadFailed(true);
			this.logError(this.log, "Error. OneWire Sensor " + this.config.alias() + " STATE:" + this.getOwdStatus().getName());
			return;
		}

		// only store values to target channel if no error occurs
		this._setTemperatureOwd(getTemperatureOwdDebug().get());
		this._setOwdReadFailed(false);		
		
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode)

		);
	}


	@Override
	public void retryModbusCommunication() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
	    return new ModbusProtocol(this); // 
	}	
}
