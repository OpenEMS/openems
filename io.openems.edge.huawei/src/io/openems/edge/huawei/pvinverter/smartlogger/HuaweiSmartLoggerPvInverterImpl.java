package io.openems.edge.huawei.pvinverter.smartlogger;

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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Huawei.SmartLogger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class HuaweiSmartLoggerPvInverterImpl extends AbstractOpenemsModbusComponent
		implements HuaweiSmartloggerPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		EventHandler, TimedataProvider, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	/**
	 * Calculates the value for total energy in [Wh_Σ].
	 */
	private final CalculateEnergyFromPower calculateTotalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	public HuaweiSmartLoggerPvInverterImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				HuaweiSmartloggerPvInverter.ChannelId.values());
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
	protected ModbusProtocol defineModbusProtocol() {

		var protocol = new ModbusProtocol(this);

		protocol.addTasks(//
				new FC3ReadRegistersTask(40554, Priority.HIGH, //
						this.m(ElectricityMeter.ChannelId.CURRENT, new SignedDoublewordElement(40554), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)), //
				new FC3ReadRegistersTask(40525, Priority.HIGH, //
						this.m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(40525), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)), //
				new FC3ReadRegistersTask(40544, Priority.HIGH, //
						this.m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(40544), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						new DummyRegisterElement(40546, 40571), //
						this.m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedWordElement(40572),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						this.m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedWordElement(40573),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						this.m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedWordElement(40574),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						this.m(HuaweiSmartloggerPvInverter.ChannelId.VOLTAGE_L1_L2, new SignedWordElement(40575),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						this.m(HuaweiSmartloggerPvInverter.ChannelId.VOLTAGE_L2_L3, new SignedWordElement(40576),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						this.m(HuaweiSmartloggerPvInverter.ChannelId.VOLTAGE_L1_L3, new SignedWordElement(40577),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3), //
						new DummyRegisterElement(40578, 40698), //
						this.m(HuaweiSmartloggerPvInverter.ChannelId.LOCKED, new SignedWordElement(40699)), //
						new DummyRegisterElement(40700, 41933), //
						this.m(HuaweiSmartloggerPvInverter.ChannelId.CAPACITY, new UnsignedDoublewordElement(41934),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)//
				));

		if (!this.config.readOnly()) {
			protocol.addTask(new FC16WriteRegistersTask(40424, //
					this.m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, //
							new UnsignedDoublewordElement(40424), //
							ElementToChannelConverter.SCALE_FACTOR_3)));
		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.calculateTotalEnergy.update(this.getActivePower().get());
		}
		}
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

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
