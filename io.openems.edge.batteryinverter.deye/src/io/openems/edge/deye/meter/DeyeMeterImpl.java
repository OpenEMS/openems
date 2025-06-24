package io.openems.edge.deye.meter;

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

import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
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
		name = "Meter.Deye", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class DeyeMeterImpl extends AbstractOpenemsModbusComponent implements DeyeMeterInternal, ElectricityMeter,
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

	private final Logger log = LoggerFactory.getLogger(DeyeMeterImpl.class);

	private Config config;

	public DeyeMeterImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DeyeMeterInternal.ChannelId.values() //
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
		return

		new ModbusProtocol(this,
				/* commented out due to timeout
				new FC3ReadRegistersTask(185, Priority.LOW,
						m(DeyeMeterInternal.ChannelId.GRID_HIGH_VOLTAGE, new SignedWordElement(185),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeMeterInternal.ChannelId.GRID_LOW_VOLTAGE, new SignedWordElement(186),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeMeterInternal.ChannelId.GRID_HIGH_FREQUENCY, new SignedWordElement(187),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeMeterInternal.ChannelId.GRID_LOW_FREQUENCY, new SignedWordElement(188),
								ElementToChannelConverter.SCALE_FACTOR_1)),
				*/
				new FC3ReadRegistersTask(598, Priority.HIGH,

						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(598),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(599),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(600),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeMeterInternal.ChannelId.VOLTAGE_L1_L2, new UnsignedWordElement(601),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeMeterInternal.ChannelId.VOLTAGE_L2_L3, new UnsignedWordElement(602),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeMeterInternal.ChannelId.VOLTAGE_L3_L1, new UnsignedWordElement(603),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(604)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(605)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(606)),

						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(607)),
						/*
						 * m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_L1_TO_GRID, new
						 * SignedWordElement(604)),
						 * m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_L2_TO_GRID, new
						 * SignedWordElement(605)),
						 * m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_L3_TO_GRID, new
						 * SignedWordElement(606)),
						 * m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_SIDE_TO_SIDE_GRID, new
						 * SignedWordElement(607)),
						 */
						m(DeyeMeterInternal.ChannelId.APPARENT_POWER_SIDE_TO_SIDE_GRID, new SignedWordElement(608)),

						m(ElectricityMeter.ChannelId.FREQUENCY, new SignedWordElement(609),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(DeyeMeterInternal.ChannelId.CURRENT_L1_TO_GRID, new SignedWordElement(610),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeMeterInternal.ChannelId.CURRENT_L2_TO_GRID, new SignedWordElement(611),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeMeterInternal.ChannelId.CURRENT_L3_TO_GRID, new SignedWordElement(612),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(DeyeMeterInternal.ChannelId.CURRENT_L1_FROM_GRID, new SignedWordElement(613),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeMeterInternal.ChannelId.CURRENT_L2_FROM_GRID, new SignedWordElement(614),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeMeterInternal.ChannelId.CURRENT_L3_FROM_GRID, new SignedWordElement(615),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_L1_FROM_GRID, new SignedWordElement(616)),
						m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_L2_FROM_GRID, new SignedWordElement(617)),
						m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_L3_FROM_GRID, new SignedWordElement(618)),

						m(DeyeMeterInternal.ChannelId.ACTIVE_POWER_FROM_GRID, new SignedWordElement(619)),
						m(DeyeMeterInternal.ChannelId.APPARENT_POWER_FROM_GRID, new SignedWordElement(620)),
						m(DeyeMeterInternal.ChannelId.GRID_CONNECTED_POWER_FACTOR, new SignedWordElement(621)
						/*
						 * m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(622)),
						 * m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(623)),
						 * m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(624)),
						 * 
						 * 
						 * m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(625)
						 */

						)));
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
				DeyeMeterInternal.ChannelId.values() //
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
				ModbusSlaveNatureTable.of(DeyeMeterInternal.class, accessMode, 100).build() //
		);
	}

}
