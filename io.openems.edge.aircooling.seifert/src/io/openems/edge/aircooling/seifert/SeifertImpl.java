package io.openems.edge.aircooling.seifert;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Aircooling.Seifert", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("Modbus")
public class SeifertImpl extends AbstractOpenemsModbusComponent
		implements Seifert, EventHandler, OpenemsComponent, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(SeifertImpl.class);

	private Config config = null;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))") //
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SeifertImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Seifert.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		this.updateTemperatureUnit();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.updateTemperatures();
		}
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0, Priority.LOW, //
						m(Seifert.ChannelId.SET_COOLING_SETPOINT, new SignedWordElement(0)), //
						m(Seifert.ChannelId.SET_HIGH_TEMP_ALARM_SETPOINT, new SignedWordElement(1)), //
						m(Seifert.ChannelId.SET_LOW_TEMP_ALARM_SETPOINT, new SignedWordElement(2)), //
						m(Seifert.ChannelId.SET_HEATING_SETPOINT, new SignedWordElement(3)), //
						new BitsWordElement(4, this) //
								.bit(8, Seifert.ChannelId.SET_ENABLE_INPUT_1_INVERT) //
								.bit(11, Seifert.ChannelId.SET_ENABLE_TEMP_UNIT), //
						m(Seifert.ChannelId.CONTROL_SETPOINT, new SignedWordElement(5)), //
						m(Seifert.ChannelId.HIGH_TEMP_SETPOINT, new SignedWordElement(6)), //
						m(Seifert.ChannelId.LOW_TEMP_SETPOINT, new SignedWordElement(7)), //
						m(Seifert.ChannelId.HEATER_SETPOINT, new SignedWordElement(8)), //
						m(Seifert.ChannelId.TEMP_POT, new SignedWordElement(9)), //
						m(Seifert.ChannelId.ALARM_POT, new SignedWordElement(10)), //
						m(Seifert.ChannelId.HEATER_POT, new SignedWordElement(11)), //
						m(Seifert.ChannelId.CONTROL_SENSOR, new SignedWordElement(12)), //
						m(Seifert.ChannelId.MODE_STATE, new UnsignedWordElement(13)), //
						new BitsWordElement(14, this) //
								.bit(0, Seifert.ChannelId.HIGH_INTERNAL_TEMP_FAULT) //
								.bit(1, Seifert.ChannelId.LOW_INTERNAL_TEMP_FAULT) //
								.bit(2, Seifert.ChannelId.PRESSURE_PIPE_ALGORITHM_FAULT) //
								.bit(3, Seifert.ChannelId.AMBIENT_SENSOR_FAULT) //
								.bit(4, Seifert.ChannelId.AUX_OR_PRESSURE_PIPE_SENSOR_FAULT) //
								.bit(5, Seifert.ChannelId.ICING_SENSOR_FAULT) //
								.bit(6, Seifert.ChannelId.INTERNAL_TEMP_SENSOR_FAULT) //
								.bit(7, Seifert.ChannelId.CAREL_INVERTER_COMMS_FAULT) //
								.bit(8, Seifert.ChannelId.PHASE_FAIL_OR_DI2_CLOSED_FAULT) //
								.bit(9, Seifert.ChannelId.COMPRESSOR_OR_ICE_ALGORITHM_FAULT) //
								.bit(10, Seifert.ChannelId.AMBIENT_BLOWER_FAULT) //
								.bit(11, Seifert.ChannelId.INTERNAL_BLOWER_FAULT) //
								.bit(12, Seifert.ChannelId.COMPRESSOR_WARNING) //
								.bit(13, Seifert.ChannelId.AMBIENT_BLOWER_WARNING) //
								.bit(14, Seifert.ChannelId.INTERNAL_BLOWER_WARNING) //
								.bit(15, Seifert.ChannelId.DELTA_OUTLET_INLET_TEMP_FAULT), //
						new BitsWordElement(15, this) //
								.bit(0, Seifert.ChannelId.OUTPUT_STATUS_HEATER) //
								.bit(1, Seifert.ChannelId.OUTPUT_STATUS_INTERNAL_BLOWER) //
								.bit(2, Seifert.ChannelId.OUTPUT_STATUS_AMBIENT_BLOWER) //
								.bit(3, Seifert.ChannelId.OUTPUT_STATUS_COMPRESSOR), //
						new BitsWordElement(16, this) //
								.bit(0, Seifert.ChannelId.DOOR_CONTACT_1_STATUS) //
								.bit(1, Seifert.ChannelId.DOOR_CONTACT_2_STATUS) //
								.bit(2, Seifert.ChannelId.DOOR_CONTACT_3_STATUS), //
						m(Seifert.ChannelId.INTERNAL_AIR_OUTLET_TEMP, new SignedWordElement(17)), //
						m(Seifert.ChannelId.AMBIENT_TEMPERATURE, new SignedWordElement(18)) //
				), //
				new FC6WriteRegisterTask(4, //
						new BitsWordElement(4, this) //
								.bit(8, Seifert.ChannelId.SET_ENABLE_INPUT_1_INVERT) //
								.bit(11, Seifert.ChannelId.SET_ENABLE_TEMP_UNIT) //
				), //
				new FC16WriteRegistersTask(0, //
						m(Seifert.ChannelId.SET_COOLING_SETPOINT, new SignedWordElement(0)), //
						m(Seifert.ChannelId.SET_HIGH_TEMP_ALARM_SETPOINT, new SignedWordElement(1)), //
						m(Seifert.ChannelId.SET_LOW_TEMP_ALARM_SETPOINT, new SignedWordElement(2)), //
						m(Seifert.ChannelId.SET_HEATING_SETPOINT, new SignedWordElement(3)) //
				) //
		);
	}

	private void updateTemperatures() {
		try {
			setWriteValueIfNotRead(getSetCoolingSetpointChannel(), this.config.coolingSetpoint());
			setWriteValueIfNotRead(getSetHeatingSetpointChannel(), this.config.heatingSetpoint());
			setWriteValueIfNotRead(getSetHighTempAlarmSetpointChannel(), this.config.highTempAlarmSetpoint());
			setWriteValueIfNotRead(getSetLowTempAlarmSetpointChannel(), this.config.lowTempAlarmSetpoint());
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "The temperatures could not be updated. " + e.getMessage());
		}
	}

	private void updateTemperatureUnit() {
		try {
			setWriteValueIfNotRead(getSetEnableTempUnitChannel(), false);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log,
					"The temperature unit for modbus table and display could not be updated. " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("State: ")//
				.append(this.getModeState())//
				.append("|AmbientTemperature: ")//
				.append(this.getAmbientTemperature())//
				.append("|InternalTemperature: ")//
				.append(this.getControlSensor())//
				.toString();
	}
}
