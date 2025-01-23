package io.openems.edge.modbus.thermometer;

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

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Modbus.Thermometer", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ModbusThermometerImpl extends AbstractOpenemsModbusComponent
		implements Thermometer, ModbusThermometer, ModbusComponent, OpenemsComponent {

	// private final Logger log =
	// LoggerFactory.getLogger(ModbusThermometerImpl.class);

	@Reference
	private ConfigurationAdmin cm;
	private Config config;
	private final Logger log = LoggerFactory.getLogger(ModbusThermometerImpl.class);

	// private Config config;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public ModbusThermometerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ModbusThermometer.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
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
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(40100, Priority.HIGH,
						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD01_DEBUG, new SignedWordElement(40100)),
						new DummyRegisterElement(40101, 40199),

						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD02_DEBUG, new SignedWordElement(40200))),

				new FC3ReadRegistersTask(40300, Priority.HIGH,
						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD03_DEBUG, new SignedWordElement(40300)),
						new DummyRegisterElement(40301, 40399),

						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD04_DEBUG, new SignedWordElement(40400))),

				new FC3ReadRegistersTask(40500, Priority.HIGH,
						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD05_DEBUG, new SignedWordElement(40500)),
						new DummyRegisterElement(40501, 40599),

						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD06_DEBUG, new SignedWordElement(40600))),

				new FC3ReadRegistersTask(40700, Priority.HIGH,
						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD07_DEBUG, new SignedWordElement(40700)),
						new DummyRegisterElement(40701, 40799),

						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD08_DEBUG, new SignedWordElement(40800))),

				new FC3ReadRegistersTask(40900, Priority.HIGH,
						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD09_DEBUG, new SignedWordElement(40900)),
						new DummyRegisterElement(40901, 40999),

						m(ModbusThermometer.ChannelId.TEMPERATURE_OWD10_DEBUG, new SignedWordElement(41000))

				));
	}

	public void handleEvent(Event event) {
		// super.handleEvent(event);

		if (event.getTopic() == EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE) {
			this.validateAndSetTemperatures();

		}

	}

	private void validateAndSetTemperatures() {
		if (this.getTemperatureOwd1Debug().get() == null || Math.abs(this.getTemperatureOwd1Debug().get() - this.getTemperatureOwd1().get()) > 1000) {
			this._setOwdReadFailed(true);
			this.logError(this.log, "Setting Temperature OWD 1 ");			
			return;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode)

		);
	}

}
