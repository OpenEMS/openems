package io.openems.edge.pytes.internalmeterbackup;

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
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
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
public class PytesInternalMeterBackupPortImpl extends AbstractOpenemsModbusComponent implements PytesInternalMeterBackupPort,
		ElectricityMeter, ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private MeterType meterType = MeterType.CONSUMPTION_METERED;

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

	private final Logger log = LoggerFactory.getLogger(PytesInternalMeterBackupPortImpl.class);

	private Config config;

	public PytesInternalMeterBackupPortImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PytesInternalMeterBackupPort.ChannelId.values() //
		);

	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.meterType = config.type();
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

				// LOW: these registers overlap with the battery block 33133-33150 that
				// is read every cycle; the fresh backup load for the control loop
				// comes from battery0/BackupLoadPower (reg 33148). This meter only
				// feeds Sum/UI, so a few seconds delay are fine. Saves one HIGH frame.
				new FC4ReadInputRegistersTask(33137, Priority.LOW,
						// reg 33137 - Backup AC voltage Phase A [mV]
						// For split-phase: L1 voltage.
						// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(33137),
								ElementToChannelConverter.SCALE_FACTOR_2), // 0.1V (Backup AC voltage Phase A / split
						// phase: L1-N) -> mV

						// reg 33138 - Backup AC current phase A [mA]
						// For split-phase: L1 current.
						// Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(33138),
								ElementToChannelConverter.SCALE_FACTOR_2), // 0.1A (Backup AC current Phase A / split
						// phase: L1-N) -> mA
						new DummyRegisterElement(33139, 33147), // Reserved

						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(33148)),
						// 1W (Backup load power, total only)

						new DummyRegisterElement(33149, 33152), // Reserved

						// reg 33153 - Backup AC voltage Phase B [mV]
						// For split-phase: L2-N voltage. For 3-phase: B phase voltage.
						// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(33153),
								ElementToChannelConverter.SCALE_FACTOR_2), // 0.1V (Backup AC voltage Phase B / split
						// phase: L2-N) -> mV

						// reg 33154 - Backup AC current Phase B [mA]
						// For split-phase: L2-N current. For 3-phase: B phase current.
						// Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(33154),
								ElementToChannelConverter.SCALE_FACTOR_2), // 0.1A (Backup AC current Phase B / split
						// phase: L2-N) -> mA

						// reg 33155 - Backup AC voltage Phase C [mV]
						// For split-phase: 0. For 3-phase: C phase voltage.
						// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(33155),
								ElementToChannelConverter.SCALE_FACTOR_2), // 0.1V (Backup AC voltage Phase C; for split
						// phase model this is 0) -> mV

						// reg 33156 - Backup AC current Phase C [mA]
						// For split-phase: 0. For 3-phase: C phase current.
						// Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mA
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(33156),
								ElementToChannelConverter.SCALE_FACTOR_2)),

				// ------------------------------------------------------------------------
				// Backup Side Per-Phase Power (reg 33521..33529)
				// Priority HIGH - real-time backup load monitoring.
				// Note: Phase C registers are always 0 for split-phase models
				// ------------------------------------------------------------------------
				new FC4ReadInputRegistersTask(33521, Priority.LOW, // per-phase details; total power comes from 33148

						// reg 33521 - Backup Phase A active power [W]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 W -> SCALE_FACTOR_1 -> W
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(33521),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33522 - Backup Phase A reactive power [Var]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 Var -> SCALE_FACTOR_1 -> Var
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(33522),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33523 - Backup Phase A apparent power [VA]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 VA -> SCALE_FACTOR_1 -> VA
						m(PytesInternalMeterBackupPort.ChannelId.APPARENT_POWER_L1, new SignedWordElement(33523),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33524 - Backup Phase B active power [W]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 W -> SCALE_FACTOR_1 -> W
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(33524),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33525 - Backup Phase B reactive power [Var]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 Var -> SCALE_FACTOR_1 -> Var
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(33525),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33526 - Backup Phase B apparent power [VA]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 VA -> SCALE_FACTOR_1 -> VA
						m(PytesInternalMeterBackupPort.ChannelId.APPARENT_POWER_L2, new SignedWordElement(33526),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33527 - Backup Phase C active power [W]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 W -> SCALE_FACTOR_1 -> W
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(33527),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33528 - Backup Phase C reactive power [Var]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 Var -> SCALE_FACTOR_1 -> Var
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(33528),
								ElementToChannelConverter.SCALE_FACTOR_1),

						// reg 33529 - Backup Phase C apparent power [VA]
						// For split-phase: using L1-N voltage and L1 current.
						// Datasheet: 10 VA -> SCALE_FACTOR_1 -> VA
						m(PytesInternalMeterBackupPort.ChannelId.APPARENT_POWER_L3, new SignedWordElement(33529),
								ElementToChannelConverter.SCALE_FACTOR_1))
		);

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
				PytesInternalMeterBackupPort.ChannelId.values() //
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
				ModbusSlaveNatureTable.of(PytesInternalMeterBackupPort.class, accessMode, 100).build() //
		);
	}

}