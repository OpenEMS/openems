package io.openems.edge.pvinverter.solarlog;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
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
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Solarlog", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PvInverterSolarlogImpl extends AbstractOpenemsModbusComponent
		implements PvInverterSolarlog, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent,
		EventHandler, ModbusSlave, TimedataProvider {

	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

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

	public PvInverterSolarlogImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvInverterSolarlog.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
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
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(3500, Priority.HIGH,
						m(PvInverterSolarlog.ChannelId.LAST_UPDATE_TIME,
								new SignedDoublewordElement(3500).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(3502).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.PDC,
								new SignedDoublewordElement(3504).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.VOLTAGE, new SignedWordElement(3506), SCALE_FACTOR_3),
						m(PvInverterSolarlog.ChannelId.UDC, new SignedWordElement(3507), SCALE_FACTOR_2),
						new DummyRegisterElement(3508, 3509), //
						m(PvInverterSolarlog.ChannelId.YESTERDAY_YIELD,
								new SignedDoublewordElement(3510).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.MONTHLY_YIELD,
								new SignedDoublewordElement(3512).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.YEARLY_YIELD,
								new SignedDoublewordElement(3514).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.TOTAL_YIELD,
								new SignedDoublewordElement(3516).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.PAC_CONSUMPTION,
								new SignedDoublewordElement(3518).wordOrder(WordOrder.LSWMSW)),
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new SignedDoublewordElement(3520).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.YESTERDAY_YIELD_CONS,
								new SignedDoublewordElement(3522).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.MONTHLY_YIELD_CONS,
								new SignedDoublewordElement(3524).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.YEARLY_YIELD_CONS,
								new SignedDoublewordElement(3526).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.TOTAL_YIELD_CONS,
								new SignedDoublewordElement(3528).wordOrder(WordOrder.LSWMSW)),
						m(PvInverterSolarlog.ChannelId.TOTAL_POWER,
								new SignedDoublewordElement(3530).wordOrder(WordOrder.LSWMSW))),

				new FC16WriteRegistersTask(10400, //
						m(PvInverterSolarlog.ChannelId.P_LIMIT_TYPE, new UnsignedWordElement(10400)),
						m(PvInverterSolarlog.ChannelId.P_LIMIT_PERC, new UnsignedWordElement(10401),
								new ChannelMetaInfoReadAndWrite(10901, 10401))),
				new FC16WriteRegistersTask(10404,
						m(PvInverterSolarlog.ChannelId.WATCH_DOG_TAG,
								new UnsignedDoublewordElement(10404).wordOrder(WordOrder.LSWMSW))),

				new FC4ReadInputRegistersTask(10900, Priority.LOW, //
						m(PvInverterSolarlog.ChannelId.STATUS, new SignedWordElement(10900)), //
						m(PvInverterSolarlog.ChannelId.P_LIMIT_PERC, new SignedWordElement(10901),
								new ChannelMetaInfoReadAndWrite(10901, 10401)),
						m(PvInverterSolarlog.ChannelId.P_LIMIT, new SignedWordElement(10902))));
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

				this.channel(PvInverterSolarlog.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(PvInverterSolarlog.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateProductionEnergy.update(this.getActivePower().get());
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
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
