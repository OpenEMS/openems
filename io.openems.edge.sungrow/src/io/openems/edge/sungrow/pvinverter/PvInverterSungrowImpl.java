package io.openems.edge.sungrow.pvinverter;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Sungrow", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PvInverterSungrowImpl extends AbstractOpenemsModbusComponent implements ManagedSymmetricPvInverter,
		ElectricityMeter, PvInverterSungrow, ModbusComponent, TimedataProvider, OpenemsComponent, EventHandler {

	private static final ElementToChannelConverter ON_OFF_CONVERTER = new ElementToChannelConverter(//
			// Element to Channel
			value -> //
			(Integer) value == 0xAA, //
			// Channel to Element
			value -> { //
				if ((Boolean) value) {
					return 0xAA;
				}
				return 0xEE;
			});

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	public PvInverterSungrowImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PvInverterSungrow.ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private boolean readOnly = true; // oEMS add

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.readOnly = config.readOnly(); // oEMS add
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.getActivePowerChannel().onSetNextValue(value -> { //
			var powerPerPhase = value.orElse(0) / 3;
			this._setActivePowerL1(powerPerPhase);
			this._setActivePowerL2(powerPerPhase);
			this._setActivePowerL3(powerPerPhase);
		});
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy.update(this.getActivePower().get());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		var protocol = new ModbusProtocol(this);
		protocol.addTasks(//
				new FC4ReadInputRegistersTask(4989, Priority.HIGH, //
						m(PvInverterSungrow.ChannelId.SERIAL_NUMBER, new StringWordElement(4989, 10)), //
						new DummyRegisterElement(4999), // Device Type Code
						m(PvInverterSungrow.ChannelId.NOMINAL_OUTPUT_POWER, new UnsignedWordElement(5000), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5001), // Outout type
						m(PvInverterSungrow.ChannelId.DAILY_ENERGY, new UnsignedWordElement(5002), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5003, 5004), //
						m(PvInverterSungrow.ChannelId.TOTAL_RUNNING_TIME,
								new UnsignedDoublewordElement(5005).wordOrder(WordOrder.LSWMSW)), //
						m(PvInverterSungrow.ChannelId.INTERNAL_TEMPERATURE, new SignedWordElement(5007)), //
						m(PvInverterSungrow.ChannelId.APPARENT_POWER,
								new UnsignedDoublewordElement(5008).wordOrder(WordOrder.LSWMSW)), //
						m(PvInverterSungrow.ChannelId.DC_VOLTAGE_1, new UnsignedWordElement(5010), //
								SCALE_FACTOR_MINUS_1), //
						m(PvInverterSungrow.ChannelId.DC_CURRENT_1, new UnsignedWordElement(5011), //
								SCALE_FACTOR_2), //
						m(PvInverterSungrow.ChannelId.DC_VOLTAGE_2, new UnsignedWordElement(5012), //
								SCALE_FACTOR_MINUS_1), //
						m(PvInverterSungrow.ChannelId.DC_CURRENT_2, new UnsignedWordElement(5013), //
								SCALE_FACTOR_2), //
						m(PvInverterSungrow.ChannelId.DC_VOLTAGE_3, new UnsignedWordElement(5014), //
								SCALE_FACTOR_MINUS_1), //
						m(PvInverterSungrow.ChannelId.DC_CURRENT_3, new UnsignedWordElement(5015), //
								SCALE_FACTOR_2), //
						m(PvInverterSungrow.ChannelId.DC_POWER,
								new UnsignedDoublewordElement(5016).wordOrder(WordOrder.LSWMSW)), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(5018), //
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(5019), //
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(5020), //
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(5021), //
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(5022), //
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(5023), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5024, 5029), // Reserved
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new UnsignedDoublewordElement(5030).wordOrder(WordOrder.LSWMSW)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(5032).wordOrder(WordOrder.LSWMSW)), //
						m(PvInverterSungrow.ChannelId.POWER_FACTOR, new SignedWordElement(5034)), //
						new DummyRegisterElement(5035, 5036), // 5035: Frequency, read from another register
						m(PvInverterSungrow.ChannelId.WORK_STATE, new UnsignedWordElement(5037)) //
				),

				new FC4ReadInputRegistersTask(5145, Priority.LOW, //
						m(PvInverterSungrow.ChannelId.NEGATIVE_VOLTAGE_TO_THE_GROUND, new SignedWordElement(5145), //
								SCALE_FACTOR_MINUS_1), //
						m(PvInverterSungrow.ChannelId.BUS_VOLTAGE, new UnsignedWordElement(5146), //
								SCALE_FACTOR_MINUS_1), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(5147), //
								SCALE_FACTOR_1) //
				),

				new FC3ReadRegistersTask(5006, Priority.LOW, //
						m(PvInverterSungrow.ChannelId.POWER_LIMITATION_SWITCH, new UnsignedWordElement(5006), //
								ON_OFF_CONVERTER), //
						m(PvInverterSungrow.ChannelId.POWER_LIMITATION_SETTING, new UnsignedWordElement(5007), //
								SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(5038, Priority.HIGH, //
						m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, new UnsignedWordElement(5038), //
								SCALE_FACTOR_2))); //
		if (!this.readOnly) { // oEMS add
			protocol.addTask(new FC6WriteRegisterTask(5038, m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, //
					new UnsignedWordElement(5038), SCALE_FACTOR_2)));
		}
		return protocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	// oEMS add
	@Override
	public boolean isReadOnly() {
		return this.readOnly;
	}
}