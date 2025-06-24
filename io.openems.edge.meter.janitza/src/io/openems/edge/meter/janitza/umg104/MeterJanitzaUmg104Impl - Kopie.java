package io.openems.edge.meter.janitza.umg104;

<<<<<<< HEAD
=======
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
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
<<<<<<< HEAD
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
=======
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
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
<<<<<<< HEAD
import io.openems.common.types.MeterType;

/**
 * Implements the Janitza UMG104 power analyzer.
 *
 * <p>
 * 
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Janitza.UMG104", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterJanitzaUmg104Impl extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave,MeterJanitzaUmg104 {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}	
	
=======

/**
 * Implements the Janitza UMG 104 power analyzer.
 *
 * <p>
 * https://www.janitza.de/umg-104-pro.html
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Janitza.UMG104", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterJanitzaUmg104Impl extends AbstractOpenemsModbusComponent
		implements MeterJanitzaUmg104, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(//
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.PRODUCTION;
	/** Invert power values. */
	private boolean invert = false;

>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
	public MeterJanitzaUmg104Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterJanitzaUmg104.ChannelId.values() //
		);
<<<<<<< HEAD
		
		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);		
	}



	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
=======

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
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

<<<<<<< HEAD

=======
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
<<<<<<< HEAD
		/*
		 * We are using the FLOAT registers from the modbus table, because they are all
		 * reachable within one ReadMultipleRegistersRequest.
		 */
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(19000, Priority.HIGH, //
						m(new FloatDoublewordElement(19000)) //
								.m(ElectricityMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.SCALE_FACTOR_3) //
								.m(ElectricityMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_3) //
								.build(), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(19002),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(19004),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(19006, 19011), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(19012),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(19014),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(19016),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(19018),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(19020),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(19022),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(19024),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(19026),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(19028, 19035), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(19036),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(19038),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(19040),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(19042),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(19044, 19049), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(19050),
								ElementToChannelConverter.SCALE_FACTOR_3)));

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
=======
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1317, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(1317), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(1319), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(1321), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(1323, 1324), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(1325), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(1327), //
								SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(1329), //
								SCALE_FACTOR_3), //
						new DummyRegisterElement(1331, 1332), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(1333), //
								INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(1335), //
								INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(1337), //
								INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(1339, 1340), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(1341), //
								INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(1343), //
								INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(1345), //
								INVERT_IF_TRUE(this.invert)), //
						new DummyRegisterElement(1347, 1368), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(1369), //
								INVERT_IF_TRUE(this.invert)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(1371), //
								INVERT_IF_TRUE(this.invert))), //
				new FC3ReadRegistersTask(1439, Priority.LOW, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(1439), //
								SCALE_FACTOR_3),
						new DummyRegisterElement(1441, 1448),
						m(MeterJanitzaUmg104.ChannelId.ROTATION_FIELD, new FloatDoublewordElement(1449), //
								SCALE_FACTOR_3),
						new DummyRegisterElement(1451, 1460),
						m(MeterJanitzaUmg104.ChannelId.INTERNAL_TEMPERATURE, new FloatDoublewordElement(1461), //
								SCALE_FACTOR_3)));

		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(9851, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(9851)),
					new DummyRegisterElement(9853, 9862),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(9863))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(9851, Priority.LOW, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(9851)),
					new DummyRegisterElement(9853, 9862),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(9863))));
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
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
<<<<<<< HEAD
				ElectricityMeter.getModbusSlaveNatureTable(accessMode)
		);

=======
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
	}
}
