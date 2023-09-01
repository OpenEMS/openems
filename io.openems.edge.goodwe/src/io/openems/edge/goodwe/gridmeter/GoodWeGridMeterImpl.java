package io.openems.edge.goodwe.gridmeter;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Grid-Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GoodWeGridMeterImpl extends AbstractOpenemsModbusComponent implements GoodWeGridMeter, ElectricityMeter,
		ModbusComponent, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

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

	public GoodWeGridMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				GoodWeGridMeter.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //

				// States
				new FC3ReadRegistersTask(36003, Priority.LOW,
						m(new UnsignedWordElement(36003)).build().onUpdateCallback((value) -> {
							this.convertMeterConnectStatus(value);
						}),

						m(GoodWeGridMeter.ChannelId.HAS_NO_METER, new UnsignedWordElement(36004),
								new ElementToChannelConverter(value -> {
									Integer intValue = TypeUtils.getAsType(OpenemsType.INTEGER, value);
									if (intValue != null) {
										switch (intValue) {
										case 0:
											return true;
										case 1:
											return false;
										}
									}
									return null;
								}))), //

				new FC3ReadRegistersTask(35123, Priority.LOW, //
						m(GoodWeGridMeter.ChannelId.F_GRID_R, new UnsignedWordElement(35123), SCALE_FACTOR_MINUS_2), // //
						new DummyRegisterElement(35124, 35127), //
						m(GoodWeGridMeter.ChannelId.F_GRID_S, new UnsignedWordElement(35128), SCALE_FACTOR_MINUS_2),
						new DummyRegisterElement(35129, 35132),
						m(GoodWeGridMeter.ChannelId.F_GRID_T, new UnsignedWordElement(35133), SCALE_FACTOR_MINUS_2), //
						m(GoodWeGridMeter.ChannelId.P_GRID_T, new SignedDoublewordElement(35134),
								SCALE_FACTOR_MINUS_2)), //

				new FC3ReadRegistersTask(36005, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(36005), INVERT), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(36006), INVERT), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(36007), INVERT), //
						new DummyRegisterElement(36008, 36012), //
						m(GoodWeGridMeter.ChannelId.METER_POWER_FACTOR, new UnsignedWordElement(36013),
								SCALE_FACTOR_MINUS_2), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(36014), SCALE_FACTOR_1)));

		// Handles different DSP versions
		ModbusUtils.readELementOnce(protocol, new UnsignedWordElement(35016), true) //
				.thenAccept(dspVersion -> {
					try {
						if (dspVersion >= 4) {
							this.handleDspVersion4(protocol);
						}

						// Handle beta versions
						if (dspVersion == 0) {

							// Handle GoodWe Types
							ModbusUtils.readELementOnce(protocol, new StringWordElement(35003, 8), true) //
									.thenAccept(serialNr -> {
										try {
											Integer version = TypeUtils.getAsType(OpenemsType.INTEGER,
													serialNr.substring(2, 4));

											// Handle GoodWe 20-30
											if (version != null && version > 20) {
												this.handleDspVersion4(protocol);
											}

										} catch (OpenemsException e) {
											this.logError(this.log, "Unable to add task for modbus protocol");
											e.printStackTrace();
										}
									});
						}

					} catch (OpenemsException e) {
						this.logError(this.log, "Unable to add task for modbus protocol");
						e.printStackTrace();
					}
				});

		return protocol;
	}

	/**
	 * Adds Registers that are available from DSP version 4.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void handleDspVersion4(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
				new FC3ReadRegistersTask(36052, Priority.LOW, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(36052), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(36053), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(36054), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(36055), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(36056), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(36057), SCALE_FACTOR_2))); //
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
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

	protected void convertMeterConnectStatus(Integer value) {
		if (value == null) {
			value = 0x0;
		}
		this.updateMeterConnectStatus(//
				GoodWeGridMeter.ChannelId.METER_CON_CORRECTLY_L1, //
				GoodWeGridMeter.ChannelId.METER_CON_INCORRECTLY_L1, //
				GoodWeGridMeter.ChannelId.METER_CON_REVERSE_L1, //
				GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L1, value));

		this.updateMeterConnectStatus(//
				GoodWeGridMeter.ChannelId.METER_CON_CORRECTLY_L2, //
				GoodWeGridMeter.ChannelId.METER_CON_INCORRECTLY_L2, //
				GoodWeGridMeter.ChannelId.METER_CON_REVERSE_L2, //
				GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L2, value));

		this.updateMeterConnectStatus(//
				GoodWeGridMeter.ChannelId.METER_CON_CORRECTLY_L3, //
				GoodWeGridMeter.ChannelId.METER_CON_INCORRECTLY_L3, //
				GoodWeGridMeter.ChannelId.METER_CON_REVERSE_L3, //
				GoodWeGridMeterImpl.getPhaseConnectionValue(Phase.L3, value));
	}

	/**
	 * Get the connection value depending on the phase.
	 * 
	 * <p>
	 * The information of each phase connection is part of a hex. The part of the
	 * given phase will be returned.
	 * 
	 * <p>
	 * For example: 0x0124 means Phase R connect incorrectly,Phase S connect
	 * reverse, Phase T connect correctly
	 * 
	 * @param phase Phase
	 * @param value Original value with all phase information
	 * @return connection information of the given phase
	 */
	protected static Integer getPhaseConnectionValue(Phase phase, int value) {
		switch (phase) {
		case L1:
			return value & 0xF;
		case L2:
			return value >> 4 & 0xF;
		case L3:
			return value >> 8 & 0xF;
		case ALL:
		default:
			return null;
		}
	}

	/**
	 * Update the connect state of the given phase.
	 * 
	 * <p>
	 * 1: connect correctly, 2: connect reverse(CT), 4:connect incorrectly,
	 * 
	 * @param correctlyChannel   correctlyChannel
	 * @param incorrectlyChannel incorrectlyChannel
	 * @param reverseChannel     reverseChannel
	 * @param value              value
	 */
	private void updateMeterConnectStatus(GoodWeGridMeter.ChannelId correctlyChannel,
			GoodWeGridMeter.ChannelId incorrectlyChannel, GoodWeGridMeter.ChannelId reverseChannel, Integer value) {

		boolean correctly = false;
		boolean incorrectly = false;
		boolean reverse = false;

		if (value != null) {

			switch (value) {
			case 4:
				incorrectly = true;
				break;
			case 2:
				reverse = true;
				break;
			case 1:
				correctly = true;
				break;
			}
		}

		this.channel(correctlyChannel).setNextValue(correctly);
		this.channel(incorrectlyChannel).setNextValue(incorrectly);
		this.channel(reverseChannel).setNextValue(reverse);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
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
				ModbusSlaveNatureTable.of(GoodWeGridMeter.class, accessMode, 100).build() //
		);
	}

}
