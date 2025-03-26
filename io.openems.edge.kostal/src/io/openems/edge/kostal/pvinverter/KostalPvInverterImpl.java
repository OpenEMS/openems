package io.openems.edge.kostal.pvinverter;

import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
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

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "PV-Inverter.Kostal.Plenticore", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION", //
				EventConstants.EVENT_TOPIC + "="
						+ EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class KostalPvInverterImpl extends AbstractOpenemsModbusComponent
		implements
			KostalPvInverter,
			ManagedSymmetricPvInverter,
			// ManagedSymmetricBatteryInverter,
			// EssDcCharger,
			ElectricityMeter,
			ModbusComponent,
			OpenemsComponent,
			ModbusSlave,
			EventHandler,
			TimedataProvider {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	protected Config config;

	public KostalPvInverterImpl() {
		super(
				//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				// EssDcCharger.ChannelId.values(),
				ManagedSymmetricPvInverter.ChannelId.values(), //
				KostalPvInverter.ChannelId.values() //
		);
		ElectricityMeter.calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;

		// Stop if component is disabled
		if (!config.enabled()) {
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
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(152, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.FREQUENCY,
								new FloatDoublewordElement(152)
										.wordOrder(LSWMSW)), //
						m(ElectricityMeter.ChannelId.CURRENT_L1,
								new FloatDoublewordElement(154)
										.wordOrder(LSWMSW)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
								new FloatDoublewordElement(156)
										.wordOrder(LSWMSW)),
						// new DummyRegisterElement(156, 157), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1,
								new FloatDoublewordElement(158)
										.wordOrder(LSWMSW)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2,
								new FloatDoublewordElement(160)
										.wordOrder(LSWMSW)), // //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
								new FloatDoublewordElement(162)
										.wordOrder(LSWMSW)),
						// new DummyRegisterElement(162, 163), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2,
								new FloatDoublewordElement(164)
										.wordOrder(LSWMSW)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3,
								new FloatDoublewordElement(166)
										.wordOrder(LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
								new FloatDoublewordElement(168)
										.wordOrder(LSWMSW))), //

				new FC3ReadRegistersTask(170, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3,
								new FloatDoublewordElement(170)
										.wordOrder(LSWMSW))), //

				new FC3ReadRegistersTask(320, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new FloatDoublewordElement(320)
										.wordOrder(LSWMSW))),

				new FC3ReadRegistersTask(531, Priority.LOW, //
						m(ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER,
								new UnsignedWordElement(531))), //

				// //322 PV daily yield, float
				// new FC3ReadRegistersTask(1056, Priority.HIGH, //
				// m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
				// new FloatDoublewordElement(1056)
				// .wordOrder(LSWMSW))),

				new FC3ReadRegistersTask(1066, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new FloatDoublewordElement(1066)
										.wordOrder(LSWMSW))));

		// TODO reactive power / active power - battery power?

		// new FC3ReadRegistersTask(1066, Priority.HIGH, //
		// m(EssDcCharger.ChannelId.ACTUAL_POWER,
		// new FloatDoublewordElement(1066)
		// .wordOrder(LSWMSW))));

	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
		// return "L:" + this.getActualPower().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(
				//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter
						.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
				// TODO do something here...
				break;
		}
	}
}
