package io.openems.edge.huawei.pvinverter;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;

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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Huawei", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class HuaweiPvInverterImpl extends AbstractOpenemsModbusComponent implements HuaweiPvInverter,
		ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public HuaweiPvInverterImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				HuaweiPvInverter.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setMaxApparentPower(config.maxApparentPower());
		ElectricityMeter.calculatePhasesFromActivePower(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		var protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(32274, Priority.HIGH, //
						this.m(HuaweiPvInverter.ChannelId.VOLTAGE_L1_L2, new UnsignedWordElement(32274), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())), //
						this.m(HuaweiPvInverter.ChannelId.VOLTAGE_L2_L3, new UnsignedWordElement(32275), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())), //
						this.m(HuaweiPvInverter.ChannelId.VOLTAGE_L3_L1, new UnsignedWordElement(32276), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(32277), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(32278), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(32279), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())), //
						this.m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(32280), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())),
						this.m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(32281), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())),
						this.m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(32282), //
								new ElementToChannelScaleFactorConverter(this.config.scaleFactor())),
						this.m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(32283), //
								SCALE_FACTOR_1), //
						new DummyRegisterElement(32284, 32289), //
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(32290)), //
						this.m(ElectricityMeter.ChannelId.REACTIVE_POWER, new UnsignedDoublewordElement(32292)), //
						this.m(HuaweiPvInverter.ChannelId.INPUT_POWER, new UnsignedDoublewordElement(32294)), //
						new DummyRegisterElement(32296, 32305), //
						this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new UnsignedDoublewordElement(32306), //
								SCALE_FACTOR_1))); // TODO: Calculate Energy From Power

		if (!this.config.readOnly()) {

			// The Write Task will fail if the Inverter sleeps during the night.
			this.addPowerListener();
			protocol.addTask(//
					new FC6WriteRegisterTask(40235, //
							this.m(HuaweiPvInverter.ChannelId.HUAWEI_ACTIVE_POWER_LIMIT, new UnsignedWordElement(40235), //
									SCALE_FACTOR_2)));
		}

		return protocol;
	}

	private void addPowerListener() {
		// Catch sleeping inverter and sets Write to null to avoid a Modbus Fault.
		this.getActivePowerLimitChannel().onSetNextWrite(value -> {
			var powerLimit = this.getActivePower().orElse(0) > 0 ? value : null;
			this.getHuaweiActivePowerLimitChannel().setNextWriteValue(powerLimit);
		});
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}
}
