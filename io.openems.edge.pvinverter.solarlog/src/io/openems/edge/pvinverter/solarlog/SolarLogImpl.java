package io.openems.edge.pvinverter.solarlog;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Solarlog", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SolarLogImpl extends AbstractOpenemsModbusComponent implements SolarLog, ManagedSymmetricPvInverter,
		ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public SolarLogImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				SolarLog.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this._setMaxApparentPower(config.maxActivePower());

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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(3500, Priority.HIGH,
						m(SolarLog.ChannelId.LAST_UPDATE_TIME,
								new SignedDoublewordElement(3500).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(3502).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.PDC, new SignedDoublewordElement(3504).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.VOLTAGE, new SignedWordElement(3506),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(SolarLog.ChannelId.UDC, new SignedWordElement(3507),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new SignedDoublewordElement(3508).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.YESTERDAY_YIELD,
								new SignedDoublewordElement(3510).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.MONTHLY_YIELD,
								new SignedDoublewordElement(3512).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.YEARLY_YIELD,
								new SignedDoublewordElement(3514).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.TOTAL_YIELD,
								new SignedDoublewordElement(3516).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.PAC_CONSUMPTION,
								new SignedDoublewordElement(3518).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new SignedDoublewordElement(3520).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.YESTERDAY_YIELD_CONS,
								new SignedDoublewordElement(3522).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.MONTHLY_YIELD_CONS,
								new SignedDoublewordElement(3524).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.YEARLY_YIELD_CONS,
								new SignedDoublewordElement(3526).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.TOTAL_YIELD_CONS,
								new SignedDoublewordElement(3528).wordOrder(WordOrder.LSWMSW)),
						m(SolarLog.ChannelId.TOTAL_POWER,
								new SignedDoublewordElement(3530).wordOrder(WordOrder.LSWMSW))),

				new FC16WriteRegistersTask(10400, //
						m(SolarLog.ChannelId.P_LIMIT_TYPE, new UnsignedWordElement(10400)),
						m(SolarLog.ChannelId.P_LIMIT_PERC, new UnsignedWordElement(10401),
								new ChannelMetaInfoReadAndWrite(10901, 10401))),
				new FC16WriteRegistersTask(10404,
						m(SolarLog.ChannelId.WATCH_DOG_TAG,
								new UnsignedDoublewordElement(10404).wordOrder(WordOrder.LSWMSW))),

				new FC4ReadInputRegistersTask(10900, Priority.LOW, //
						m(SolarLog.ChannelId.STATUS, new SignedWordElement(10900)), //
						m(SolarLog.ChannelId.P_LIMIT_PERC, new SignedWordElement(10901),
								new ChannelMetaInfoReadAndWrite(10901, 10401)),
						m(SolarLog.ChannelId.P_LIMIT, new SignedWordElement(10902))));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(SolarLog.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(SolarLog.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarLog.class, accessMode, 100) //
						.build());
	}
}
