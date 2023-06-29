package io.openems.edge.meter.janitza.umg96rme;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

/**
 * Implements the Janitza UMG 96RM-E power analyzer.
 *
 * <p>
 * https://www.janitza.com/umg-96rm-e.html
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Janitza.UMG96RME", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterJanitzaUmg96rmeImpl extends AbstractOpenemsModbusComponent
		implements MeterJanitzaUmg96rme, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.PRODUCTION;
	/** Invert power values. */
	private boolean invert = false;

	public MeterJanitzaUmg96rmeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterJanitzaUmg96rme.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		/*
		 * We are using the FLOAT registers from the modbus table, because they are all
		 * reachable within one ReadMultipleRegistersRequest.
		 */
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(800, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(800), SCALE_FACTOR_3),
						new DummyRegisterElement(802, 807), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(808), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(810), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(812), SCALE_FACTOR_3),
						new DummyRegisterElement(814, 859), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(860), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(862), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(864), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(866), SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(868),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(870),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(872),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(874),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(876),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(878),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(880),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(882),
								INVERT_IF_TRUE(this.invert))));

		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(19068, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19068)),
					new DummyRegisterElement(19070, 19075),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19076))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(19068, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19068)),
					new DummyRegisterElement(19070, 19075),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19076))));
		}

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}
}
