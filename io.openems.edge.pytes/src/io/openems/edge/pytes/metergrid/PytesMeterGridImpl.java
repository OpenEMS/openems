package io.openems.edge.pytes.metergrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pytes.enums.MeterDeviceType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Pytes.Grid", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PytesMeterGridImpl extends AbstractOpenemsModbusComponent implements PytesMeterGrid, ElectricityMeter,
		ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final Logger log = LoggerFactory.getLogger(PytesMeterGridImpl.class);

	private Config config;

	public PytesMeterGridImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PytesMeterGrid.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		this.meterType = config.type();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		
		this.installListeners();

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

		var modbusProtocol = new ModbusProtocol(this,
				
				new FC4ReadInputRegistersTask(33248, Priority.LOW, // total: 45 registers
						m(new BitsWordElement(33248, this)
						.bit(0, PytesMeterGrid.ChannelId.EPM_SWITCH)
						.bit(1, PytesMeterGrid.ChannelId.FAILSAFE_SWITCH)),
						new DummyRegisterElement(33249, 33249),				

						m(new BitsWordElement(33250, this).bit(1, PytesMeterGrid.ChannelId.METER_IN_GRID)
								.bit(2, PytesMeterGrid.ChannelId.CT_IN_GRID)
								.bit(4, PytesMeterGrid.ChannelId.EPM_SWITCH_STATUS)
								.bit(5, PytesMeterGrid.ChannelId.FAILSAFE_SWITCH_STATUS)
								.bit(7, PytesMeterGrid.ChannelId.METER_FAULT_STATUS)
								.bit(8, PytesMeterGrid.ChannelId.CT_FAULT_STATUS)
								.bit(9, PytesMeterGrid.ChannelId.METER_REVERSE_STATUS)
								.bit(10, PytesMeterGrid.ChannelId.CT_REVERSE_STATUS)
								.bit(11, PytesMeterGrid.ChannelId.EPM_FAULT_STATUS)
								.bit(12, PytesMeterGrid.ChannelId.POWER_CONTROL_MODE_UNBALANCED_ALLOWED)),
						new DummyRegisterElement(33251, 33286),
						m(PytesMeterGrid.ChannelId.OPERATING_STATUS, new UnsignedWordElement(33287)), // 33287
						new DummyRegisterElement(33288, 33289),
						m(PytesMeterGrid.ChannelId.CT_SELFTEST_RESULT, new UnsignedWordElement(33290)), // 33290
						new DummyRegisterElement(33291, 33291),
						m(PytesMeterGrid.ChannelId.EQUIPMENT_FAULT_CODE, new UnsignedWordElement(33292)) // 33292						

		));

			modbusProtocol.addTask(new FC3ReadRegistersTask(43073, Priority.LOW,
					m(new BitsWordElement(43073, this)
							.bit(2,  PytesMeterGrid.ChannelId.METER_CT_IN_GRID)
							.bit(3,  PytesMeterGrid.ChannelId.METER_PARALLEL_PV_CT_SWITCH)
							.bit(4,  PytesMeterGrid.ChannelId.METER_EPM_SWITCH)
							.bit(5,  PytesMeterGrid.ChannelId.METER_FAILSAFE_SWITCH)
							.bit(6,  PytesMeterGrid.ChannelId.METER_POWER_CONTROL_MODE_UNBALANCED)
							.bit(7,  PytesMeterGrid.ChannelId.METER_EPM_CURRENT_SETTING_SWITCH)
							.bit(8,  PytesMeterGrid.ChannelId.METER_EXTERNAL_EPM_STATUS)
							.bit(9,  PytesMeterGrid.ChannelId.METER_EXTERNAL_EPM_FAILSAFE_SWITCH)
							.bit(13, PytesMeterGrid.ChannelId.METER_CT_SELECTION))
			));
		
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(33300, Priority.LOW, 
			    m(PytesMeterGrid.ChannelId.METER1_TYPE_LOCATION_RAW, new UnsignedWordElement(33300))));
/*			
		if (this.config.meterDeviceType() == MeterDeviceType.INTERNAL) {
			// Inverter Grid Electrical (33073..33094)
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(33073, Priority.LOW, // total: 22 registers
					m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(33073),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(33074),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(33075),
							ElementToChannelConverter.SCALE_FACTOR_2),

					m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(33076),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(33077),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(33078),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(33079)),
					m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(33081)),
					m(PytesMeterGrid.ChannelId.APPARENT_POWER, new SignedDoublewordElement(33083)),
					new DummyRegisterElement(33085, 33093),
					m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(33094))));
		} else {
*/		
			// External meter / EPM Grid Electrical (33250..33282 / 33286)
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(33251, Priority.LOW,

					m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(33251),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(33252),
							ElementToChannelConverter.SCALE_FACTOR_1),
					m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(33253),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(33254),
							ElementToChannelConverter.SCALE_FACTOR_1),
					m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(33255),
							ElementToChannelConverter.SCALE_FACTOR_2),
					m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(33256),
							ElementToChannelConverter.SCALE_FACTOR_1),

					m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(33257),
							ElementToChannelConverter.INVERT),
					m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(33259),
							ElementToChannelConverter.INVERT),
					m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(33261),
							ElementToChannelConverter.INVERT),
					m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(33263),
							ElementToChannelConverter.INVERT),

					m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(33265)),
					m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(33267)),
					m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(33269)),
					m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(33271)),

					m(PytesMeterGrid.ChannelId.APPARENT_POWER_L1, new SignedDoublewordElement(33273)),
					m(PytesMeterGrid.ChannelId.APPARENT_POWER_L2, new SignedDoublewordElement(33275)),
					m(PytesMeterGrid.ChannelId.APPARENT_POWER_L3, new SignedDoublewordElement(33277)),
					m(PytesMeterGrid.ChannelId.APPARENT_POWER, new SignedDoublewordElement(33279)),
					m(PytesMeterGrid.ChannelId.METER_PF, new SignedWordElement(33281),
							ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
					m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(33282),ElementToChannelConverter.SCALE_FACTOR_1)
					
					));
		//}

		return modbusProtocol;

	}


	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			this.logDebug();
			break;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
	
	
	private void installListeners() {

	    this.getMeter1TypeLocationRawChannel().onUpdate(value -> {
	        // value ist Value<Integer>
	        Integer raw = (value == null) ? null : value.get();

	        if (raw == null) {
	            this.channel(PytesMeterGrid.ChannelId.METER1_LOCATION_CODE).setNextValue(null);
	            this.channel(PytesMeterGrid.ChannelId.METER1_TYPE_CODE).setNextValue(null);
	            if (this.config.debugMode()) {
	                this.logDebug(this.log, "METER1_TYPE_LOCATION_RAW is null -> cleared derived channels");
	            }
	            return;
	        }

	        int locCode  = (raw >>> 8) & 0xFF;
	        int typeCode = raw & 0xFF;

	        this.channel(PytesMeterGrid.ChannelId.METER1_LOCATION_CODE).setNextValue(locCode);
	        this.channel(PytesMeterGrid.ChannelId.METER1_TYPE_CODE).setNextValue(typeCode);

	        if (this.config.debugMode()) {
	            this.logDebug(this.log, "METER1_TYPE_LOCATION_RAW=0x" + Integer.toHexString(raw)
	                    + " -> location=" + locCode + " type=" + typeCode);
	        }
	    });

	}

	

	public String collectDebugData() {
		// Collect channel values in one stream
		return Stream.of(OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PytesMeterGrid.ChannelId.values() //
		).flatMap(Arrays::stream).map(id -> {
			try {
				return id.name() + "=" + this.channel(id).value().asString();
			} catch (Exception e) {
				return id.name() + "=n/a";
			}
		}).collect(Collectors.joining("; \n"));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	protected void logDebug() {
		if (this.config.debugMode()) {

			if (this.config.extendedDebugMode()) {
				this.logInfo(this.log,
						"\n ############################################## Meter Values Start #############################################");
				this.logInfo(log, this.collectDebugData());
				this.logInfo(log,
						"\n ############################################## Meter Values End #############################################");

			}

		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(PytesMeterGrid.class, accessMode, 100).build() //
		);
	}

}
