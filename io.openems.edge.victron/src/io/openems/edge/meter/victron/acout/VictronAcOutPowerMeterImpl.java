package io.openems.edge.meter.victron.acout;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE;

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
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Victron.AcOut", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class VictronAcOutPowerMeterImpl extends AbstractOpenemsModbusComponent implements VictronAcOutPowerMeter,
		ElectricityMeter, OpenemsComponent, ModbusSlave, EventHandler, TimedataProvider {

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	public VictronAcOutPowerMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				VictronAcOutPowerMeter.ChannelId.values(), ModbusComponent.ChannelId.values() //
		);
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumReactivePowerFromPhases(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

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
		return this.config.type();
	}

	private void setEnergyValues() {
		var activePower = this.getActivePower().get();

		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else if (activePower < 0) {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		} else {
			// Undefined
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public void handleEvent(Event event) {

		this.setEnergyValues();

	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(15, Priority.HIGH, //

						// Output Voltages
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(15),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(16),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(17),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),

						// Output Currents
						this.m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedWordElement(18),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						this.m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedWordElement(19),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),
						this.m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedWordElement(20),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(this.config.invert())),

						// Output Frequency
						this.m(ElectricityMeter.ChannelId.FREQUENCY, new SignedWordElement(21),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),

						new DummyRegisterElement(22),

						// Output Powers
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(23),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())),
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(24),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())),
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(25),
								SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert())),
						new DummyRegisterElement(26, 73),

						// Be careful! Values reset after system reboot
						this.m(VictronAcOutPowerMeter.ChannelId.ENERGY_FROM_AC_IN_1_TO_AC_OUT,
								new UnsignedDoublewordElement(74), ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(76, 77),
						this.m(VictronAcOutPowerMeter.ChannelId.ENERGY_FROM_AC_IN_2_TO_AC_OUT,
								new UnsignedDoublewordElement(78), ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(80, 81),
						this.m(VictronAcOutPowerMeter.ChannelId.ENERGY_FROM_AC_OUT_TO_AC_IN_1,
								new UnsignedDoublewordElement(82), ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronAcOutPowerMeter.ChannelId.ENERGY_FROM_AC_OUT_TO_AC_IN_2,
								new UnsignedDoublewordElement(84), ElementToChannelConverter.SCALE_FACTOR_1),
						new DummyRegisterElement(86, 89),
						this.m(VictronAcOutPowerMeter.ChannelId.ENERGY_FROM_BATTERY_TO_AC_OUT,
								new UnsignedDoublewordElement(90), ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(VictronAcOutPowerMeter.ChannelId.ENERGY_FROM_AC_OUT_TO_BATTERY,
								new UnsignedDoublewordElement(92), ElementToChannelConverter.SCALE_FACTOR_1)

				));
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

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
