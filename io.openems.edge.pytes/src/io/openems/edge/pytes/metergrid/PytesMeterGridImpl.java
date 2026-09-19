package io.openems.edge.pytes.metergrid;

import java.util.Arrays;
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
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
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

	private MeterType meterType = MeterType.GRID;

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

		this.meterType = config.type();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
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

				// ---------------------------------------------------------------
				// EPM / CT status flags (reg 33248-33250)
				// Priority LOW - status/diagnostic, polled infrequently
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33248, Priority.LOW,

						// reg 33248 - EPM and Failsafe hardware switch states
						// BIT00 = EPM switch, BIT01 = Failsafe switch
						m(new BitsWordElement(33248, this)
						.bit(0, PytesMeterGrid.ChannelId.EPM_SWITCH)
						.bit(1, PytesMeterGrid.ChannelId.FAILSAFE_SWITCH)),

						// reg 33249 - EPM real time backflow power (not mapped)
						new DummyRegisterElement(33249, 33249),

						// reg 33250 - Meter/CT position and EPM status flags (Appendix 10)
						m(new BitsWordElement(33250, this)
								.bit(1,  PytesMeterGrid.ChannelId.METER_IN_GRID)
								.bit(2,  PytesMeterGrid.ChannelId.CT_IN_GRID)
								.bit(4,  PytesMeterGrid.ChannelId.EPM_SWITCH_STATUS)
								.bit(5,  PytesMeterGrid.ChannelId.FAILSAFE_SWITCH_STATUS)
								.bit(6,  PytesMeterGrid.ChannelId.POWER_CONTROL_MODE_UNBALANCED)
								.bit(7,  PytesMeterGrid.ChannelId.EPM_CURRENT_SETTING_SWITCH_STATUS)
								.bit(8,  PytesMeterGrid.ChannelId.EXTERNAL_EPM_STATUS)
								.bit(9,  PytesMeterGrid.ChannelId.EXTERNAL_EPM_FAILSAFE_STATUS)
								.bit(10, PytesMeterGrid.ChannelId.PARALLEL_EPM_POWER_SETTING_SWITCH)
								.bit(11, PytesMeterGrid.ChannelId.PARALLEL_EPM_CURRENT_SETTING_SWITCH)
								.bit(12, PytesMeterGrid.ChannelId.PARALLEL_POWER_CONTROL_MODE_UNBALANCED))
				),

				// ---------------------------------------------------------------
				// CT self-test and equipment fault code (reg 33290-33292)
				// ---------------------------------------------------------------
				new FC4ReadInputRegistersTask(33290, Priority.LOW,

						// reg 33290 - CT self-test result
						// 0=Not tested, 1=Not meeting conditions, 2=Testing,
						// 3=Normal, 100=Abnormal CT connection
						m(PytesMeterGrid.ChannelId.CT_SELFTEST_RESULT, new UnsignedWordElement(33290)),

						// reg 33291 – reserved
						new DummyRegisterElement(33291, 33291),

						// reg 33292 - Equipment fault sub-code
						//Used together with reg 33095 to identify specific fault
						m(PytesMeterGrid.ChannelId.EQUIPMENT_FAULT_CODE, new UnsignedWordElement(33292))

				)
		);

		// ---------------------------------------------------------------
		// External meter electrical measurements (reg 33251-33282)
		// Priority HIGH - real-time grid power monitoring
		// These are the CT/meter readings at the grid connection point
		// ---------------------------------------------------------------
		modbusProtocol.addTask(new FC4ReadInputRegistersTask(33251, Priority.HIGH,

				// reg 33251 - Meter Phase A voltage [mV]
				// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
				m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(33251),
						ElementToChannelConverter.SCALE_FACTOR_2),

				// reg 33252 - Meter Phase A current [mA]
				// Datasheet: 0.01 A -> SCALE_FACTOR_1 -> mA
				m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(33252),
						ElementToChannelConverter.SCALE_FACTOR_1),

				// reg 33253 - Meter Phase B voltage [mV]
				// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
				m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(33253),
						ElementToChannelConverter.SCALE_FACTOR_2),

				// reg 33254 - Meter Phase B current [mA]
				// Datasheet: 0.01 A -> SCALE_FACTOR_1 -> mA
				m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(33254),
						ElementToChannelConverter.SCALE_FACTOR_1),

				// reg 33255 - Meter Phase C voltage [mV]
				// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
				m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(33255),
						ElementToChannelConverter.SCALE_FACTOR_2),

				// reg 33256 - Meter Phase C current [mA]
				// Datasheet: 0.01 A -> SCALE_FACTOR_1 -> mA
				m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(33256),
						ElementToChannelConverter.SCALE_FACTOR_1),

				// reg 33257-33258 - Meter Phase A active power [W] (S32)
				// Datasheet: 0.001kw = 1 W -> no converter needed
				// INVERT: meter convention positive = export, OpenEMS = import
				m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(33257),
						ElementToChannelConverter.INVERT),

				// reg 33259-33260 - Meter Phase B active power [W] (S32)
				m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(33259),
						ElementToChannelConverter.INVERT),

				// reg 33261-33262 - Meter Phase C active power [W] (S32)
				m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(33261),
						ElementToChannelConverter.INVERT),

				// reg 33263-33264 - Meter total active power [W] (S32)
				m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(33263),
						ElementToChannelConverter.INVERT),

				// reg 33265-332566 - Meter Phase A reactive power [Var] (S32)
				// Datasheet: 1 Var -> no converter needed
				m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(33265)),

				// reg 33267-332568 - Meter Phase B reactive power [Var] (S32)
				m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(33267)),

				// reg 33269-332570 - Meter Phase C reactive power [Var] (S32)
				m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(33269)),

				// reg 33271-332572 - Meter total reactive power [Var] (S32)
				m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(33271)),

				// reg 33273-332574 - Meter Phase A apparent power [VA] (S32)
				// Datasheet: 1 VA -> no converter needed
				m(PytesMeterGrid.ChannelId.APPARENT_POWER_L1, new SignedDoublewordElement(33273)),

				// reg 33275-332576 - Meter Phase B apparent power [VA] (S32)
				m(PytesMeterGrid.ChannelId.APPARENT_POWER_L2, new SignedDoublewordElement(33275)),

				// reg 33277-332578 - Meter Phase C apparent power [VA] (S32)
				m(PytesMeterGrid.ChannelId.APPARENT_POWER_L3, new SignedDoublewordElement(33277)),

				// reg 33279-332580 - Meter total apparent power [VA] (S32)
				m(PytesMeterGrid.ChannelId.APPARENT_POWER, new SignedDoublewordElement(33279)),

				// reg 332581 - Meter power factor (cos phi) (S16)
				// Datasheet: 0.01 -> SCALE_FACTOR_MINUS_2
				// Range: -1.0 to -0.8 and +0.8 to +1.0
				m(PytesMeterGrid.ChannelId.METER_PF, new SignedWordElement(33281),
						ElementToChannelConverter.SCALE_FACTOR_MINUS_2),

				// reg 332582 - Meter grid frequency [mHz]
				// Datasheet: 0.01 Hz -> SCALE_FACTOR_1
				m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(33282),
						ElementToChannelConverter.SCALE_FACTOR_1)

		));

		// ---------------------------------------------------------------
		// Meter type and location (reg 33300)
		// Priority LOW - config data, rarely changes
		// ---------------------------------------------------------------
		modbusProtocol.addTask(new FC4ReadInputRegistersTask(33300, Priority.LOW,
				// reg 33300 - Meter 1 type and location
				// High byte = location
				// Low byte = device type
				// Decoded by listener into METER1_LOCATION_CODE and METER1_TYPE_CODE
				m(PytesMeterGrid.ChannelId.METER1_TYPE_LOCATION_RAW, new UnsignedWordElement(33300))
		));

		// ---------------------------------------------------------------
		// METER/CT Position register (reg 43073, holding register FC3)
		// Priority LOW - configuration register, polled infrequently
		// Appendix 12 bitmask: EPM, failsafe, CT selection settings
		// ---------------------------------------------------------------
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

		
		// ---------------------------------------------------------------
		// Backflow power (reg 43074)
		// ---------------------------------------------------------------
		modbusProtocol.addTask(new FC3ReadRegistersTask(43074, Priority.LOW, // setting, rarely changes

			    m(PytesMeterGrid.ChannelId.BACKFLOW_POWER, new SignedWordElement(43074))
		));		

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
	 * Calculate energy from active power each cycle.
	 * Positive active power = buying from grid (production energy counter)
	 * Negative active power = selling to grid (consumption energy counter)
	 */
	private void calculateEnergy() {
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
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

	    // Decode reg 33300 raw word into location and type sub-channels
	    this.getMeter1TypeLocationRawChannel().onUpdate(value -> {
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

		// Reconstruct METER_CT_POSITION_RAW (reg 43073) from individual decoded bit channels.
		// This keeps the raw word available for diagnostics or write-back without
		// needing a second Modbus read of the same register.
		// Cast to BooleanReadChannel explicitly — generic type inference via channel() does
		// not work with ChannelId enums in this OpenEMS version.
		BooleanReadChannel chCtInGrid     = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_CT_IN_GRID);
		BooleanReadChannel chParallelPv   = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_PARALLEL_PV_CT_SWITCH);
		BooleanReadChannel chEpmSw        = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_EPM_SWITCH);
		BooleanReadChannel chFailsafeSw   = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_FAILSAFE_SWITCH);
		BooleanReadChannel chPcmUnbal     = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_POWER_CONTROL_MODE_UNBALANCED);
		BooleanReadChannel chEpmCurrSw    = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_EPM_CURRENT_SETTING_SWITCH);
		BooleanReadChannel chExtEpmStatus = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_EXTERNAL_EPM_STATUS);
		BooleanReadChannel chExtFailsafe  = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_EXTERNAL_EPM_FAILSAFE_SWITCH);
		BooleanReadChannel chCtSelect     = (BooleanReadChannel) this.channel(PytesMeterGrid.ChannelId.METER_CT_SELECTION);

		Runnable rebuildMeterCtRaw = () -> {
			int word = 0;
			Boolean ctInGrid     = chCtInGrid.value().get();
			Boolean parallelPv   = chParallelPv.value().get();
			Boolean epmSw        = chEpmSw.value().get();

			if (ctInGrid     != null && ctInGrid) {
				word |= (1 << 2);
			}
			if (parallelPv   != null && parallelPv) {
				word |= (1 << 3);
			}
			if (epmSw        != null && epmSw) {
				word |= (1 << 4);
			}

			Boolean failsafeSw = chFailsafeSw.value().get();
			if (failsafeSw   != null && failsafeSw) {
				word |= (1 << 5);
			}

			Boolean pcmUnbal = chPcmUnbal.value().get();
			if (pcmUnbal     != null && pcmUnbal) {
				word |= (1 << 6);
			}

			Boolean epmCurrSw = chEpmCurrSw.value().get();
			if (epmCurrSw    != null && epmCurrSw) {
				word |= (1 << 7);
			}

			Boolean extEpmStatus = chExtEpmStatus.value().get();
			if (extEpmStatus != null && extEpmStatus) {
				word |= (1 << 8);
			}

			Boolean extFailsafe = chExtFailsafe.value().get();
			if (extFailsafe  != null && extFailsafe) {
				word |= (1 << 9);
			}

			Boolean ctSelect = chCtSelect.value().get();
			if (ctSelect     != null && ctSelect) {
				word |= (1 << 13);
			}

			this.channel(PytesMeterGrid.ChannelId.METER_CT_POSITION_RAW).setNextValue(word);
			if (this.config.debugMode()) {
				this.logDebug(this.log, "METER_CT_POSITION_RAW (43073) reconstructed=0x" + Integer.toHexString(word));
			}
		};

		chCtInGrid.onUpdate(v -> rebuildMeterCtRaw.run());
		chParallelPv.onUpdate(v -> rebuildMeterCtRaw.run());
		chEpmSw.onUpdate(v -> rebuildMeterCtRaw.run());
		chFailsafeSw.onUpdate(v -> rebuildMeterCtRaw.run());
		chPcmUnbal.onUpdate(v -> rebuildMeterCtRaw.run());
		chEpmCurrSw.onUpdate(v -> rebuildMeterCtRaw.run());
		chExtEpmStatus.onUpdate(v -> rebuildMeterCtRaw.run());
		chExtFailsafe.onUpdate(v -> rebuildMeterCtRaw.run());
		chCtSelect.onUpdate(v -> rebuildMeterCtRaw.run());
	}



	/**
	 * Collects the current values of all Channels for debug logging.
	 *
	 * @return a formatted string of all Channel values
	 */
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
				this.logInfo(this.log, this.collectDebugData());
				this.logInfo(this.log,
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