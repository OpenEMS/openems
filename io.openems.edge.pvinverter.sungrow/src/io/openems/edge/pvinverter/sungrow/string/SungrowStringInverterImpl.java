package io.openems.edge.pvinverter.sungrow.string;

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
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE })
@Component(//
		name = "String-Inverter.Sungrow", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SungrowStringInverterImpl extends AbstractOpenemsModbusComponent implements SungrowStringInverter,
		ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider {

	private static final ElementToChannelConverter ON_OFF_CONVERTER = new ElementToChannelConverter((value) -> {
		if ((Integer) value == 0xAA) {
			return true;
		}
		return false;
	}, (value) -> {
		if ((Boolean) value) {
			return 0xAA;
		}
		return 0xEE;
	});

	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	public SungrowStringInverterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SungrowStringInverter.ChannelId.values() //
		);
		ElectricityMeter.PhasesWithVoltAndAmpere(this);

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
		} else if (activePower >= 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
		}
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private MeterType meterType = MeterType.GRID;

	private boolean invertActivePower;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invertActivePower = config.invertActivePower();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ElementToChannelConverter converter = this.invertActivePower ? ElementToChannelConverter.INVERT
				: ElementToChannelConverter.DIRECT_1_TO_1;

		return new ModbusProtocol(this, new FC4ReadInputRegistersTask(4989, Priority.HIGH,

				m(SungrowStringInverter.ChannelId.SERIAL_NUMBER, new StringWordElement(4989, 10)),

				m(SungrowStringInverter.ChannelId.DEVICE_CODE, new UnsignedWordElement(4999)),

				m(SungrowStringInverter.ChannelId.NOMINAL_OUTPUT_POWER, new UnsignedWordElement(5000), //
						ElementToChannelConverter.SCALE_FACTOR_2), //

				m(SungrowStringInverter.ChannelId.OUTPUT_TYPE, new UnsignedWordElement(5001)),

				m(SungrowStringInverter.ChannelId.DAILY_ENERGY, new UnsignedWordElement(5002), //
						ElementToChannelConverter.SCALE_FACTOR_2), //

				m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
						new UnsignedDoublewordElement(5003).wordOrder(WordOrder.LSWMSW), //
						ElementToChannelConverter.SCALE_FACTOR_3), //

				m(SungrowStringInverter.ChannelId.TOTAL_RUNNING_TIME,
						new UnsignedDoublewordElement(5005).wordOrder(WordOrder.LSWMSW)), //

				m(SungrowStringInverter.ChannelId.INTERNAL_TEMPERATURE, new SignedWordElement(5007),
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //

				m(SungrowStringInverter.ChannelId.APPARENT_POWER,
						new UnsignedDoublewordElement(5008).wordOrder(WordOrder.LSWMSW)), //

				m(SungrowStringInverter.ChannelId.DC_VOLTAGE_1, new UnsignedWordElement(5010), //
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //

				m(SungrowStringInverter.ChannelId.DC_CURRENT_1, new UnsignedWordElement(5011), //
						ElementToChannelConverter.SCALE_FACTOR_2), //

				m(SungrowStringInverter.ChannelId.DC_VOLTAGE_2, new UnsignedWordElement(5012), //
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //

				m(SungrowStringInverter.ChannelId.DC_CURRENT_2, new UnsignedWordElement(5013), //
						ElementToChannelConverter.SCALE_FACTOR_2), //

				m(SungrowStringInverter.ChannelId.DC_VOLTAGE_3, new UnsignedWordElement(5014), //
						ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //

				m(SungrowStringInverter.ChannelId.DC_CURRENT_3, new UnsignedWordElement(5015), //
						ElementToChannelConverter.SCALE_FACTOR_2), //

				m(SungrowStringInverter.ChannelId.DC_POWER,
						new UnsignedDoublewordElement(5016).wordOrder(WordOrder.LSWMSW)), //

				m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(5018), //
						ElementToChannelConverter.SCALE_FACTOR_2), //
				m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(5019), //
						ElementToChannelConverter.SCALE_FACTOR_2), //
				m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(5020), //
						ElementToChannelConverter.SCALE_FACTOR_2), //
				m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(5021), //
						ElementToChannelConverter.SCALE_FACTOR_2), //
				m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(5022), //
						ElementToChannelConverter.SCALE_FACTOR_2), //
				m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(5023), //
						ElementToChannelConverter.SCALE_FACTOR_2), //
				new DummyRegisterElement(5024, 5029),
				m(ElectricityMeter.ChannelId.ACTIVE_POWER,
						new UnsignedDoublewordElement(5030).wordOrder(WordOrder.LSWMSW), converter),
				m(ElectricityMeter.ChannelId.REACTIVE_POWER,
						new SignedDoublewordElement(5032).wordOrder(WordOrder.LSWMSW)), //
				m(SungrowStringInverter.ChannelId.POWER_FACTOR, new SignedWordElement(5034)), //
				new DummyRegisterElement(5035, 5036), // 5035: Frequency, read from another register, 5036: reserved
				m(SungrowStringInverter.ChannelId.WORK_STATE, new UnsignedWordElement(5037)) // ,
		// new DummyRegisterElement(5038, 5090),
		// m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION, new
		// SignedDoublewordElement(5091)) //

		),

				new FC4ReadInputRegistersTask(5145, Priority.LOW, //
						m(SungrowStringInverter.ChannelId.NEGATIVE_VOLTAGE_TO_THE_GROUND, new SignedWordElement(5145), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SungrowStringInverter.ChannelId.BUS_VOLTAGE, new UnsignedWordElement(5146), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(5147), //
								ElementToChannelConverter.SCALE_FACTOR_1) //
				),

				new FC3ReadRegistersTask(5006, Priority.LOW, //
						m(SungrowStringInverter.ChannelId.POWER_LIMITATION_SWITCH, new UnsignedWordElement(5006), //
								ON_OFF_CONVERTER), //
						m(SungrowStringInverter.ChannelId.POWER_LIMITATION_SETTING, new UnsignedWordElement(5007), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(5038, Priority.HIGH, //
						m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, new UnsignedWordElement(5038), //
								ElementToChannelConverter.SCALE_FACTOR_2)), //

				new FC6WriteRegisterTask(5038, m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, //
						new UnsignedWordElement(5038), ElementToChannelConverter.SCALE_FACTOR_2)));
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
	public MeterType getMeterType() {
		return this.meterType;
	}
}