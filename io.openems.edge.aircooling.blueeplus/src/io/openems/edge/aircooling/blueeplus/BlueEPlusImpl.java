package io.openems.edge.aircooling.blueeplus;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "AirCooling.BlueEPlus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class BlueEPlusImpl extends AbstractOpenemsModbusComponent
		implements BlueEPlus, OpenemsComponent, ModbusComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))" //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public BlueEPlusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				BlueEPlus.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC16WriteRegistersTask(0, //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_HIGH_FAULT, new UnsignedDoublewordElement(0)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_HIGH_WARNING,
								new UnsignedDoublewordElement(2)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_LOW_WARNING, new SignedDoublewordElement(4)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_LOW_FAULT, new SignedDoublewordElement(6)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_HYSTERESIS, new UnsignedDoublewordElement(8)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_CATEGORY, new UnsignedDoublewordElement(10)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_HIGH_ALARM, new UnsignedDoublewordElement(12),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_HIGH_WARNING,
								new UnsignedDoublewordElement(14), SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_LOW_WARNING, new SignedDoublewordElement(16),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_LOW_ALARM, new SignedDoublewordElement(18),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_HYSTERESIS, new UnsignedDoublewordElement(20)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_CATEGORY, new UnsignedDoublewordElement(22)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_HIGH_ALARM,
								new UnsignedDoublewordElement(24)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_HIGH_WARNING,
								new UnsignedDoublewordElement(26)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_LOW_WARNING,
								new SignedDoublewordElement(28)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_LOW_ALARM,
								new SignedDoublewordElement(30)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_HYSTERESIS,
								new UnsignedDoublewordElement(32)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_CATEGORY, new UnsignedDoublewordElement(34)), //
						m(BlueEPlus.ChannelId.MONITORING_COOLING_CATEGORY, new UnsignedDoublewordElement(36)), //
						m(BlueEPlus.ChannelId.MONITORING_INTERNAL_AIR_CIRCUIT_CATEGORY,
								new UnsignedDoublewordElement(38)), //
						m(BlueEPlus.ChannelId.MONITORING_EXTERNAL_AIR_CIRCUIT_CATEGORY,
								new UnsignedDoublewordElement(40)), //
						m(BlueEPlus.ChannelId.MONITORING_INTERNAL_FAN_CATEGORY, new UnsignedDoublewordElement(42)), //
						m(BlueEPlus.ChannelId.MONITORING_EXTERNAL_FAN_CATEGORY, new UnsignedDoublewordElement(44)), //
						m(BlueEPlus.ChannelId.MONITORING_COMPRESSOR_CATEGORY, new UnsignedDoublewordElement(46)), //
						m(BlueEPlus.ChannelId.MONITORING_EEV_CATEGORY, new UnsignedDoublewordElement(48)), //
						m(BlueEPlus.ChannelId.MONITORING_FILTER_CATEGORY, new UnsignedDoublewordElement(50)), //
						m(BlueEPlus.ChannelId.MONITORING_DOOR_CATEGORY, new UnsignedDoublewordElement(52)), //
						m(BlueEPlus.ChannelId.MONITORING_ELECTRONICS_CATEGORY, new UnsignedDoublewordElement(54)), //
						m(BlueEPlus.ChannelId.MONITORING_CONDENSATE_CATEGORY, new UnsignedDoublewordElement(56)), //
						m(BlueEPlus.ChannelId.MONITORING_SYSTEM_MESSAGES_CATEGORY, new UnsignedDoublewordElement(58)), //
						m(BlueEPlus.ChannelId.SETUP_MODE, new UnsignedDoublewordElement(60)), //
						m(BlueEPlus.ChannelId.SETUP_ALARM_TOLERANCE_FILTER, new UnsignedDoublewordElement(62)), //
						m(BlueEPlus.ChannelId.SETUP_STANDARD_CONTROL_SETPOINT, new UnsignedDoublewordElement(64),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.SETUP_STANDARD_CONTROL_ALARM_THRESHOLD, new UnsignedDoublewordElement(66),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.SETUP_OUTLET_TEMPERATURE_SETPOINT, new UnsignedDoublewordElement(68),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.SETUP_OUTLET_TEMPERATURE_ALARM_THRESHOLD,
								new UnsignedDoublewordElement(70), SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_COMMAND, new UnsignedDoublewordElement(72)), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_TIMEOUT, new UnsignedDoublewordElement(74)), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_TIMEOUT_REQUIRED, new UnsignedDoublewordElement(76)), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_CATEGORY, new UnsignedDoublewordElement(78)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_VALUE, new UnsignedDoublewordElement(80)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_TIMEOUT, new UnsignedDoublewordElement(82)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_TIMEOUT_REQUIRED, new UnsignedDoublewordElement(84)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_CATEGORY, new UnsignedDoublewordElement(86)) //
				), //
				new FC3ReadRegistersTask(0, Priority.LOW, //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_HIGH_FAULT, new UnsignedDoublewordElement(0)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_HIGH_WARNING,
								new UnsignedDoublewordElement(2)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_LOW_WARNING, new SignedDoublewordElement(4)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_SET_PT_LOW_FAULT, new SignedDoublewordElement(6)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_HYSTERESIS, new UnsignedDoublewordElement(8)), //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_CATEGORY, new UnsignedDoublewordElement(10)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_HIGH_ALARM, new UnsignedDoublewordElement(12)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_HIGH_WARNING,
								new UnsignedDoublewordElement(14)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_LOW_WARNING, new SignedDoublewordElement(16)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_SET_PT_LOW_ALARM, new SignedDoublewordElement(18)), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_HYSTERESIS, new UnsignedDoublewordElement(20),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_CATEGORY, new UnsignedDoublewordElement(22)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_HIGH_ALARM,
								new UnsignedDoublewordElement(24)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_HIGH_WARNING,
								new UnsignedDoublewordElement(26)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_LOW_WARNING,
								new SignedDoublewordElement(28)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_SET_PT_LOW_ALARM,
								new SignedDoublewordElement(30)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_HYSTERESIS,
								new UnsignedDoublewordElement(32)), //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_CATEGORY, new UnsignedDoublewordElement(34)), //
						m(BlueEPlus.ChannelId.MONITORING_COOLING_CATEGORY, new UnsignedDoublewordElement(36)), //
						m(BlueEPlus.ChannelId.MONITORING_INTERNAL_AIR_CIRCUIT_CATEGORY,
								new UnsignedDoublewordElement(38)), //
						m(BlueEPlus.ChannelId.MONITORING_EXTERNAL_AIR_CIRCUIT_CATEGORY,
								new UnsignedDoublewordElement(40)), //
						m(BlueEPlus.ChannelId.MONITORING_INTERNAL_FAN_CATEGORY, new UnsignedDoublewordElement(42)), //
						m(BlueEPlus.ChannelId.MONITORING_EXTERNAL_FAN_CATEGORY, new UnsignedDoublewordElement(44)), //
						m(BlueEPlus.ChannelId.MONITORING_COMPRESSOR_CATEGORY, new UnsignedDoublewordElement(46)), //
						m(BlueEPlus.ChannelId.MONITORING_EEV_CATEGORY, new UnsignedDoublewordElement(48)), //
						m(BlueEPlus.ChannelId.MONITORING_FILTER_CATEGORY, new UnsignedDoublewordElement(50)), //
						m(BlueEPlus.ChannelId.MONITORING_DOOR_CATEGORY, new UnsignedDoublewordElement(52)), //
						m(BlueEPlus.ChannelId.MONITORING_ELECTRONICS_CATEGORY, new UnsignedDoublewordElement(54)), //
						m(BlueEPlus.ChannelId.MONITORING_CONDENSATE_CATEGORY, new UnsignedDoublewordElement(56)), //
						m(BlueEPlus.ChannelId.MONITORING_SYSTEM_MESSAGES_CATEGORY, new UnsignedDoublewordElement(58)), //
						m(BlueEPlus.ChannelId.SETUP_MODE, new UnsignedDoublewordElement(60)), //
						m(BlueEPlus.ChannelId.SETUP_ALARM_TOLERANCE_FILTER, new UnsignedDoublewordElement(62)), //
						m(BlueEPlus.ChannelId.SETUP_STANDARD_CONTROL_SETPOINT, new UnsignedDoublewordElement(64),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.SETUP_STANDARD_CONTROL_ALARM_THRESHOLD,
								new UnsignedDoublewordElement(66)), //
						m(BlueEPlus.ChannelId.SETUP_OUTLET_TEMPERATURE_SETPOINT, new UnsignedDoublewordElement(68)), //
						m(BlueEPlus.ChannelId.SETUP_OUTLET_TEMPERATURE_ALARM_THRESHOLD,
								new UnsignedDoublewordElement(70), SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_COMMAND, new UnsignedDoublewordElement(72)), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_TIMEOUT, new UnsignedDoublewordElement(74)), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_TIMEOUT_REQUIRED, new UnsignedDoublewordElement(76)), //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_CATEGORY, new UnsignedDoublewordElement(78)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_VALUE, new UnsignedDoublewordElement(80),
								SCALE_FACTOR_MINUS_2), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_TIMEOUT, new UnsignedDoublewordElement(82)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_TIMEOUT_REQUIRED, new UnsignedDoublewordElement(84)), //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_CATEGORY, new UnsignedDoublewordElement(86)) //
				), //
				new FC4ReadInputRegistersTask(3038, Priority.LOW, //
						m(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_STATUS, new UnsignedDoublewordElement(3038)) //
				), //
				new FC4ReadInputRegistersTask(3167, Priority.LOW, //
						m(BlueEPlus.ChannelId.AMBIENT_TEMPERATURE_STATUS, new UnsignedDoublewordElement(3167)) //
				), //
				new FC4ReadInputRegistersTask(3296, Priority.LOW, //
						m(BlueEPlus.ChannelId.EXTERNAL_TEMPERATURE_SENSOR_STATUS, new UnsignedDoublewordElement(3296)) //
				), //
				new FC4ReadInputRegistersTask(3728, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_COOLING_STATUS, new UnsignedDoublewordElement(3728)) //
				), //
				new FC4ReadInputRegistersTask(3857, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_INTERNAL_AIR_CIRCUIT_STATUS,
								new UnsignedDoublewordElement(3857)) //
				), //
				new FC4ReadInputRegistersTask(3986, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_EXTERNAL_AIR_CIRCUIT_STATUS,
								new UnsignedDoublewordElement(3986)) //
				), //
				new FC4ReadInputRegistersTask(4173, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_INTERNAL_FAN_STATUS, new UnsignedDoublewordElement(4173)) //
				), //
				new FC4ReadInputRegistersTask(4360, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_EXTERNAL_FAN_STATUS, new UnsignedDoublewordElement(4360)) //
				), //
				new FC4ReadInputRegistersTask(4547, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_COMPRESSOR_STATUS, new UnsignedDoublewordElement(4547)) //
				), //
				new FC4ReadInputRegistersTask(4734, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_EEV_STATUS, new UnsignedDoublewordElement(4734)) //
				), //
				new FC4ReadInputRegistersTask(4805, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_FILTER_STATUS, new UnsignedDoublewordElement(4805)) //
				), //
				new FC4ReadInputRegistersTask(4876, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_DOOR_STATUS, new UnsignedDoublewordElement(4876)) //
				), //
				new FC4ReadInputRegistersTask(4947, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_ELECTRONICS_STATUS, new UnsignedDoublewordElement(4947)) //
				), //
				new FC4ReadInputRegistersTask(5018, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_CONDENSATE_STATUS, new UnsignedDoublewordElement(5018)) //
				), //
				new FC4ReadInputRegistersTask(5089, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_SYSTEM_MESSAGES_STATUS, new UnsignedDoublewordElement(5089)) //
				), //
				new FC4ReadInputRegistersTask(5624, Priority.LOW, //
						m(BlueEPlus.ChannelId.MONITORING_ERROR_LIST_STATUS, new UnsignedDoublewordElement(5624)) //
				), //
				new FC4ReadInputRegistersTask(5811, Priority.LOW, //
						m(BlueEPlus.ChannelId.REMOTE_DOOR_STATUS, new UnsignedDoublewordElement(5811)) //
				), //
				new FC4ReadInputRegistersTask(5882, Priority.LOW, //
						m(BlueEPlus.ChannelId.REMOTE_TEMPERATURE_STATUS, new UnsignedDoublewordElement(5882)) //
				) //

		);
	}

}
