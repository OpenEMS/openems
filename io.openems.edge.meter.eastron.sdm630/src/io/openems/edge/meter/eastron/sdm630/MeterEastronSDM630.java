package io.openems.edge.meter.eastron.sdm630;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Eastron.SDM630", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterEastronSDM630 extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, AsymmetricMeter, SymmetricMeter {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterEastronSDM630() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)

	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private String name;

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this._initializeMinMaxActivePower(this.cm, // Initialize Min/MaxActivePower channels
				config.service_pid(), config.minActivePower(), config.maxActivePower());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		APPARENT_POWER_L1(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L2(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L3(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.VOLT_AMPERE)), //
		FREQUENCY(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.HERTZ)), //
		ACTIVE_PRODUCTION_ENERGY(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //
		REACTIVE_PRODUCTION_ENERGY(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //
		ACTIVE_CONSUMPTION_ENERGY(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //
		REACTIVE_CONSUMPTION_ENERGY(new Doc() //
				.type(OpenemsType.FLOAT) //
				.unit(Unit.KILOWATT_HOURS)), //
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, new FC4ReadInputRegistersTask(30001, Priority.LOW,
				// VOLTAGE
				// Overall Voltage
				// measured from L1
				m(SymmetricMeter.ChannelId.VOLTAGE, new SignedDoublewordElement(30001).wordOrder(WordOrder.MSWLSW),
						ElementToChannelConverter.SCALE_FACTOR_3),
				// Phase 1 voltage
				m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new SignedDoublewordElement(30001).wordOrder(WordOrder.MSWLSW),
						ElementToChannelConverter.SCALE_FACTOR_3),
				// Phase 2 voltage
				m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new SignedDoublewordElement(30003).wordOrder(WordOrder.MSWLSW),
						ElementToChannelConverter.SCALE_FACTOR_3),
				// Phase 3 voltage
				m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new SignedDoublewordElement(30005)
						.wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3)),
				new FC4ReadInputRegistersTask(30007, Priority.HIGH,
						// CURRENT
						// Phase 1 current
						m(AsymmetricMeter.ChannelId.CURRENT_L1,
								new SignedDoublewordElement(30007).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// Phase 2 current
						m(AsymmetricMeter.ChannelId.CURRENT_L2,
								new SignedDoublewordElement(30009).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// Phase 3 current
						m(AsymmetricMeter.ChannelId.CURRENT_L3,
								new SignedDoublewordElement(30011).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_3),
						// APPARENT POWER
						// phase 1 VA
						m(MeterEastronSDM630.ChannelId.APPARENT_POWER_L1,
								new SignedDoublewordElement(30013).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 2 VA
						m(MeterEastronSDM630.ChannelId.APPARENT_POWER_L2,
								new SignedDoublewordElement(30015).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 3 VA
						m(MeterEastronSDM630.ChannelId.APPARENT_POWER_L3,
								new SignedDoublewordElement(30017).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// ACTIVE POWER
						// phase 1 active power
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1,
								new SignedDoublewordElement(30019).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 2 active power
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2,
								new SignedDoublewordElement(30021).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 3 active power
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3,
								new SignedDoublewordElement(30023).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30025, 30030),
						// REACTIVE POWER
						// phase 1 VAr
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
								new SignedDoublewordElement(30031).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 2 VAr
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
								new SignedDoublewordElement(30033).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						// phase 3 VAr
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
								new SignedDoublewordElement(30035).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30038, 30048),
						// Overall Current
						m(SymmetricMeter.ChannelId.CURRENT,
								new SignedDoublewordElement(30049).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.SCALE_FACTOR_3),
						new DummyRegisterElement(30051, 30052),
						// total system VA
						m(MeterEastronSDM630.ChannelId.APPARENT_POWER,
								new SignedDoublewordElement(30053).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30055, 30056),
						// total system active power
						m(SymmetricMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(30057).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(30059, 30060),
						// total system VAr
						m(SymmetricMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(30061).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC4ReadInputRegistersTask(30071, Priority.LOW,
						// frequency
						m(ChannelId.FREQUENCY, new SignedDoublewordElement(30071).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						//active energy import/export
						m(ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new SignedDoublewordElement(30073).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new SignedDoublewordElement(30075).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						//reactive energy import/export
						m(ChannelId.REACTIVE_PRODUCTION_ENERGY,
								new SignedDoublewordElement(30077).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChannelId.REACTIVE_CONSUMPTION_ENERGY,
								new SignedDoublewordElement(30079).wordOrder(WordOrder.MSWLSW),
								ElementToChannelConverter.DIRECT_1_TO_1)));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

}
