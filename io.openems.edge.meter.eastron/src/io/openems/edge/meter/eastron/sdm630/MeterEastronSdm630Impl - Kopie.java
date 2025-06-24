package io.openems.edge.meter.eastron.sdm630;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
<<<<<<< HEAD
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.KEEP_POSITIVE;
=======
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

import java.nio.ByteOrder;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.types.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Microcare.SDM630", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
<<<<<<< HEAD
public class MeterEastronSdm630Impl extends AbstractOpenemsModbusComponent
		implements MeterEastronSdm630, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {
=======
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class MeterEastronSdm630Impl extends AbstractOpenemsModbusComponent implements MeterEastronSdm630,
		ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave, TimedataProvider, EventHandler {

	private CalculateEnergyFromPower calculateProductionEnergy;
	private CalculateEnergyFromPower calculateConsumptionEnergy;
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244

	private MeterType meterType = MeterType.PRODUCTION;
	private boolean invert;

	@Reference
	private ConfigurationAdmin cm;

<<<<<<< HEAD
=======
	@Reference
	private ComponentManager cma;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public MeterEastronSdm630Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterEastronSdm630.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
<<<<<<< HEAD
		

		
=======
		this.invert = config.invert();

		this.calculateProductionEnergy = new CalculateEnergyFromPower(this,
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, this.cma.getClock());
		this.calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, this.cma.getClock());

>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
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
	protected ModbusProtocol defineModbusProtocol() {
		final var offset = 30001;
		
		
		// ToDo: Meter cannot be type GRID
		/**
		 * if meter is used for production Active power is positive
		 * 
		 */
		if (this.meterType == MeterType.PRODUCTION || this.meterType == MeterType.PRODUCTION_AND_CONSUMPTION) {
			var modbusProtocol = new ModbusProtocol(this, //			
				new FC4ReadInputRegistersTask(30001 - offset, Priority.HIGH,	
						m(ElectricityMeter.ChannelId.VOLTAGE_L1,
								new FloatDoublewordElement(30001 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2,
								new FloatDoublewordElement(30003 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3,
								new FloatDoublewordElement(30005 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L1,
								new FloatDoublewordElement(30007 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2,
								new FloatDoublewordElement(30009 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3,
								new FloatDoublewordElement(30011 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
								new FloatDoublewordElement(30013 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
<<<<<<< HEAD
										ElementToChannelConverter.chain(INVERT, KEEP_POSITIVE)), // Invert if production
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
								new FloatDoublewordElement(30015 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
										ElementToChannelConverter.chain(INVERT, KEEP_POSITIVE)), // Invert if production
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
								new FloatDoublewordElement(30017 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
										ElementToChannelConverter.chain(INVERT, KEEP_POSITIVE)), // Invert if production
=======
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
								new FloatDoublewordElement(30015 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
								new FloatDoublewordElement(30017 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								INVERT_IF_TRUE(this.invert)),
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
						new DummyRegisterElement(30019 - offset, 30024 - offset),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,
								new FloatDoublewordElement(30025 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,
								new FloatDoublewordElement(30027 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								INVERT_IF_TRUE(this.invert)),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,
								new FloatDoublewordElement(30029 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								INVERT_IF_TRUE(this.invert)),
						new DummyRegisterElement(30031 - offset, 30048 - offset),
						m(ElectricityMeter.ChannelId.CURRENT,
								new FloatDoublewordElement(30049 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						new DummyRegisterElement(30051 - offset, 30052 - offset),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(30053 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
<<<<<<< HEAD
										ElementToChannelConverter.chain(INVERT, KEEP_POSITIVE)), // Invert if production
=======
								INVERT_IF_TRUE(this.invert)),
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
						new DummyRegisterElement(30055 - offset, 30060 - offset),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER,
								new FloatDoublewordElement(30061 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								INVERT_IF_TRUE(this.invert)),
						new DummyRegisterElement(30063 - offset, 30070 - offset),
						m(ElectricityMeter.ChannelId.FREQUENCY,
								new FloatDoublewordElement(30071 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								DIRECT_1_TO_1)));
				
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(30073 - offset, Priority.HIGH,							
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30073 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30075 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(MeterEastronSdm630.ChannelId.REACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30077 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(MeterEastronSdm630.ChannelId.REACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30079 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3)));	
				
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(30347 - offset, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1,
								new FloatDoublewordElement(30347 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),

						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2,
								new FloatDoublewordElement(30349 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),		
						
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3,
								new FloatDoublewordElement(30351 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),						
						
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1,
								new FloatDoublewordElement(30353 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),			
						
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2,
								new FloatDoublewordElement(30355 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),	
						
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3,
								new FloatDoublewordElement(30357 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3)));
				return modbusProtocol;		
				
			} else { // Consumption-Meter darf nicht negativ werden
				var modbusProtocol = new ModbusProtocol(this, //		
						new FC4ReadInputRegistersTask(30001 - offset, Priority.HIGH,							
						m(ElectricityMeter.ChannelId.VOLTAGE_L1,
								new FloatDoublewordElement(30001 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2,
								new FloatDoublewordElement(30003 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3,
								new FloatDoublewordElement(30005 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L1,
								new FloatDoublewordElement(30007 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2,
								new FloatDoublewordElement(30009 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3,
								new FloatDoublewordElement(30011 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
								new FloatDoublewordElement(30013 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
										KEEP_POSITIVE),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
								new FloatDoublewordElement(30015 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
										KEEP_POSITIVE),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
								new FloatDoublewordElement(30017 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
										KEEP_POSITIVE),
						new DummyRegisterElement(30019 - offset, 30024 - offset),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,
								new FloatDoublewordElement(30025 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,
								new FloatDoublewordElement(30027 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								DIRECT_1_TO_1),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,
								new FloatDoublewordElement(30029 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								DIRECT_1_TO_1),
						new DummyRegisterElement(30031 - offset, 30048 - offset),
						m(ElectricityMeter.ChannelId.CURRENT,
								new FloatDoublewordElement(30049 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						new DummyRegisterElement(30051 - offset, 30052 - offset),
						
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(30053 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
										KEEP_POSITIVE),
						
						
						new DummyRegisterElement(30055 - offset, 30060 - offset),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER,
								new FloatDoublewordElement(30061 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								DIRECT_1_TO_1),
						new DummyRegisterElement(30063 - offset, 30070 - offset),
						m(ElectricityMeter.ChannelId.FREQUENCY,
								new FloatDoublewordElement(30071 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								DIRECT_1_TO_1)));	
				
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(30073 - offset, Priority.HIGH,							
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30073 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30075 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(MeterEastronSdm630.ChannelId.REACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(30077 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3),
						m(MeterEastronSdm630.ChannelId.REACTIVE_CONSUMPTION_ENERGY,
								new FloatDoublewordElement(30079 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								SCALE_FACTOR_3)));
				
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(30347 - offset, Priority.HIGH,
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1,
								new FloatDoublewordElement(30347 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),

						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2,
								new FloatDoublewordElement(30349 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),		
						
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3,
								new FloatDoublewordElement(30351 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),						
						
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1,
								new FloatDoublewordElement(30353 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),			
						
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2,
								new FloatDoublewordElement(30355 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3),	
						
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3,
								new FloatDoublewordElement(30357 - offset).wordOrder(WordOrder.MSWLSW)
										.byteOrder(ByteOrder.BIG_ENDIAN),
								ElementToChannelConverter.SCALE_FACTOR_3)));
			
				return modbusProtocol;		
			}
		

									
	}

	
	
	
	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	/**
	 * Modbus-Table for module.
	 */
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
<<<<<<< HEAD
=======

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.calculateEnergy();
		}
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}
>>>>>>> 0f7119c2c1d3028c968f8556ef526f5fcfa16244
}
