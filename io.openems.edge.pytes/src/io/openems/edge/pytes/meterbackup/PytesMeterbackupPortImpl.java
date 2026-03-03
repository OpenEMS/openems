package io.openems.edge.pytes.meterbackup;

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
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Pytes.Backup", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PytesMeterbackupPortImpl extends AbstractOpenemsModbusComponent implements PytesMeterBackupPort, ElectricityMeter,
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

	private final Logger log = LoggerFactory.getLogger(PytesMeterbackupPortImpl.class);

	private Config config;

	public PytesMeterbackupPortImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PytesMeterBackupPort.ChannelId.values() //
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

			return new ModbusProtocol(this,

					new FC4ReadInputRegistersTask(33073, Priority.HIGH,
							// -----------------------------------------------------------------------------
							// Inverter Grid Electrical (AC) – direkt am Wechselrichter (FC=0x04)
							// Register 33073..33083, 33094
							// -----------------------------------------------------------------------------

							m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(33073),
									ElementToChannelConverter.SCALE_FACTOR_2), // 0.1V  (A phase voltage / AB line voltage) -> mV

							m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(33074),
									ElementToChannelConverter.SCALE_FACTOR_2), // 0.1V  (B phase voltage / BC line voltage)

							m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(33075),
									ElementToChannelConverter.SCALE_FACTOR_2), // 0.1V  (C phase voltage / CA line voltage)

							m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(33076), 
									ElementToChannelConverter.SCALE_FACTOR_2), // 0.1A  (A phase current) -> mA

							m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(33077),
									ElementToChannelConverter.SCALE_FACTOR_2), // 0.1A  (B phase current)

							m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(33078),
									ElementToChannelConverter.SCALE_FACTOR_2), // 0.1A  (C phase current)

							m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(33079)), // 1W   (Active power)

							m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(33081)), // 1Var (Reactive power)

							m(PytesMeterBackupPort.ChannelId.APPARENT_POWER, new SignedDoublewordElement(33083)), // 1VA  (Apparent power)
							
							new DummyRegisterElement(33085, 33093), // Reserved

							m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(33094)))); // 0.01Hz (Grid frequency) -> mHz
/*

							// -----------------------------------------------------------------------------
							// Inverter AC Grid Port Power/Energy (FC=0x04)
							// Register 33151, 33169..33176, 33186..33188
							// -----------------------------------------------------------------------------

							m(PytesMeter.ChannelId.AC_GRID_PORT_ACTIVE_POWER, new SignedDoublewordElement(33151),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1W  (+ to grid, - from grid)

							m(PytesMeter.ChannelId.ENERGY_IMPORTED_FROM_GRID_TOTAL, new UnsignedDoublewordElement(33169),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1kWh (Total energy imported from grid)

							m(PytesMeter.ChannelId.ENERGY_IMPORTED_FROM_GRID_TODAY, new UnsignedWordElement(33171),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1kWh (Today energy imported from grid)

							m(PytesMeter.ChannelId.ENERGY_IMPORTED_FROM_GRID_YESTERDAY, new UnsignedWordElement(33172),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1kWh (Yesterday energy imported from grid)

							m(PytesMeter.ChannelId.ENERGY_FED_INTO_GRID_TOTAL, new UnsignedDoublewordElement(33173),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1kWh (Total energy fed into grid)

							m(PytesMeter.ChannelId.ENERGY_FED_INTO_GRID_TODAY, new UnsignedWordElement(33175),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1kWh (Today energy fed into grid)

							m(PytesMeter.ChannelId.ENERGY_FED_INTO_GRID_YESTERDAY, new UnsignedWordElement(33176),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1kWh (Yesterday energy fed into grid)

							m(PytesMeter.ChannelId.AC_GRID_PORT_EXPORT_ENERGY_TOTAL, new UnsignedDoublewordElement(33186),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1kWh (Inverter AC Grid Port Export Energy)

							m(PytesMeter.ChannelId.AC_GRID_PORT_IMPORT_ENERGY_TOTAL, new UnsignedDoublewordElement(33188),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1kWh (Inverter AC Grid Port Import Energy)


							// -----------------------------------------------------------------------------
							// External Meter / CT (Grid side OR Load side) – Meter block (FC=0x04)
							// Register 33247..33285
							// Hinweis im PDF: 33251..33286 sind "meter itself"; bei Meter auf Load-Side siehe 33540..33575.
							// -----------------------------------------------------------------------------

							m(PytesMeter.ChannelId.EPM_BACKFLOW_POWER, new SignedWordElement(33247),
									ElementToChannelConverter.SCALE_FACTOR_2), // 100W (+ to grid, - from grid)

							m(PytesMeter.ChannelId.EPM_REALTIME_BACKFLOW_POWER, new SignedWordElement(33249),
									ElementToChannelConverter.SCALE_FACTOR_2), // 100W

							m(PytesMeter.ChannelId.AC_VOLTAGE_PHASE_A, new UnsignedWordElement(33251),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1V (Meter ac voltage A)

							m(PytesMeter.ChannelId.AC_CURRENT_PHASE_A, new UnsignedWordElement(33252),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01A (Meter ac current A)

							m(PytesMeter.ChannelId.AC_VOLTAGE_PHASE_B, new UnsignedWordElement(33253),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1V (Meter ac voltage B)

							m(PytesMeter.ChannelId.AC_CURRENT_PHASE_B, new UnsignedWordElement(33254),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01A (Meter ac current B)

							m(PytesMeter.ChannelId.AC_VOLTAGE_PHASE_C, new UnsignedWordElement(33255),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1V (Meter ac voltage C)

							m(PytesMeter.ChannelId.AC_CURRENT_PHASE_C, new UnsignedWordElement(33256),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01A (Meter ac current C)

							
							//  Meter Active Power A/B/C + Total sind S32 in 0.001kW.
							//  -> Wenn Du kW als Channel willst: SCALE_FACTOR_MINUS_3
							//  -> Wenn Du W als Channel willst: SCALE_FACTOR_0 (weil 0.001kW = 1W) und dann semantisch als W behandeln.
							// 
							m(PytesMeter.ChannelId.ACTIVE_POWER_PHASE_A, new SignedDoublewordElement(33257),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3), // 0.001kW

							m(PytesMeter.ChannelId.ACTIVE_POWER_PHASE_B, new SignedDoublewordElement(33259),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3), // 0.001kW

							m(PytesMeter.ChannelId.ACTIVE_POWER_PHASE_C, new SignedDoublewordElement(33261),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3), // 0.001kW

							m(PytesMeter.ChannelId.ACTIVE_POWER_TOTAL, new SignedDoublewordElement(33263),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_3), // 0.001kW

							m(PytesMeter.ChannelId.REACTIVE_POWER_PHASE_A, new SignedDoublewordElement(33265),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.REACTIVE_POWER_PHASE_B, new SignedDoublewordElement(33267),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.REACTIVE_POWER_PHASE_C, new SignedDoublewordElement(33269),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.REACTIVE_POWER_TOTAL, new SignedDoublewordElement(33271),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.APPARENT_POWER_PHASE_A, new SignedDoublewordElement(33273),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.APPARENT_POWER_PHASE_B, new SignedDoublewordElement(33275),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.APPARENT_POWER_PHASE_C, new SignedDoublewordElement(33277),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.APPARENT_POWER_TOTAL, new SignedDoublewordElement(33279),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.POWER_FACTOR, new SignedWordElement(33281),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01 (PF)

							m(PytesMeter.ChannelId.GRID_FREQUENCY, new UnsignedWordElement(33282),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01Hz

							m(PytesMeter.ChannelId.ACTIVE_ENERGY_FROM_GRID_TOTAL, new UnsignedDoublewordElement(33283),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01kWh

							m(PytesMeter.ChannelId.ACTIVE_ENERGY_TO_GRID_TOTAL, new UnsignedDoublewordElement(33285),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01kWh


							// -----------------------------------------------------------------------------
							// Meter on LOAD-SIDE: Public Grid Side computed values (FC=0x04)
							// Register 33540..33575
							// (Nur relevant, wenn der Zähler "on the load side" installiert ist; siehe Hinweis im PDF.)
							// -----------------------------------------------------------------------------

							m(PytesMeter.ChannelId.PUBLIC_GRID_VOLTAGE_PHASE_A, new UnsignedWordElement(33540),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1V

							m(PytesMeter.ChannelId.PUBLIC_GRID_CURRENT_PHASE_A, new UnsignedWordElement(33541),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01A

							m(PytesMeter.ChannelId.PUBLIC_GRID_VOLTAGE_PHASE_B, new UnsignedWordElement(33542),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1V

							m(PytesMeter.ChannelId.PUBLIC_GRID_CURRENT_PHASE_B, new UnsignedWordElement(33543),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01A

							m(PytesMeter.ChannelId.PUBLIC_GRID_VOLTAGE_PHASE_C, new UnsignedWordElement(33544),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1), // 0.1V

							m(PytesMeter.ChannelId.PUBLIC_GRID_CURRENT_PHASE_C, new UnsignedWordElement(33545),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01A

							m(PytesMeter.ChannelId.PUBLIC_GRID_ACTIVE_POWER_PHASE_A, new SignedDoublewordElement(33546),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1W

							m(PytesMeter.ChannelId.PUBLIC_GRID_ACTIVE_POWER_PHASE_B, new SignedDoublewordElement(33548),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1W

							m(PytesMeter.ChannelId.PUBLIC_GRID_ACTIVE_POWER_PHASE_C, new SignedDoublewordElement(33550),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1W

							m(PytesMeter.ChannelId.PUBLIC_GRID_ACTIVE_POWER_TOTAL, new SignedDoublewordElement(33552),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1W

							m(PytesMeter.ChannelId.PUBLIC_GRID_REACTIVE_POWER_PHASE_A, new SignedDoublewordElement(33554),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.PUBLIC_GRID_REACTIVE_POWER_PHASE_B, new SignedDoublewordElement(33556),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.PUBLIC_GRID_REACTIVE_POWER_PHASE_C, new SignedDoublewordElement(33558),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.PUBLIC_GRID_REACTIVE_POWER_TOTAL, new SignedDoublewordElement(33560),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1Var

							m(PytesMeter.ChannelId.PUBLIC_GRID_APPARENT_POWER_PHASE_A, new SignedDoublewordElement(33562),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.PUBLIC_GRID_APPARENT_POWER_PHASE_B, new SignedDoublewordElement(33564),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.PUBLIC_GRID_APPARENT_POWER_PHASE_C, new SignedDoublewordElement(33566),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.PUBLIC_GRID_APPARENT_POWER_TOTAL, new SignedDoublewordElement(33568),
									ElementToChannelConverter.SCALE_FACTOR_0), // 1VA

							m(PytesMeter.ChannelId.PUBLIC_GRID_POWER_FACTOR, new SignedWordElement(33570),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01

							m(PytesMeter.ChannelId.PUBLIC_GRID_FREQUENCY, new UnsignedWordElement(33571),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01Hz

							m(PytesMeter.ChannelId.PUBLIC_GRID_ACTIVE_ENERGY_TAKEN_TOTAL, new UnsignedDoublewordElement(33572),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2), // 0.01kWh

							m(PytesMeter.ChannelId.PUBLIC_GRID_ACTIVE_ENERGY_DELIVERED_TOTAL, new UnsignedDoublewordElement(33574),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_2); // 0.01kWh
*/

		
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

	public String collectDebugData() {
		// Collect channel values in one stream
		return Stream.of(OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PytesMeterBackupPort.ChannelId.values() //
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
				ModbusSlaveNatureTable.of(PytesMeterBackupPort.class, accessMode, 100).build() //
		);
	}

}
