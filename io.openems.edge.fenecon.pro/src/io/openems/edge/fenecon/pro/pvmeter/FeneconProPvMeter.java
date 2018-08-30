package io.openems.edge.fenecon.pro.pvmeter;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "PvMeter.Fenecon.Pro", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
)

public class FeneconProPvMeter extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	private final static int UNIT_ID = 100;

	private String modbusBridgeId;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public FeneconProPvMeter() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		ModbusProtocol protocol = new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(118, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedWordElement(118)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedWordElement(119)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedWordElement(120)), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(121)), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(122)), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(123))), //

				new FC3ReadRegistersTask(2035, Priority.HIGH, // //
						m(FeneconProPvMeter.ChannelId.ACTIVE_ENERGY_L1, new UnsignedDoublewordElement(2035)), //
						new DummyRegisterElement(2037, 2065), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(2066))), //
				new FC3ReadRegistersTask(2135, Priority.HIGH, // //
						m(FeneconProPvMeter.ChannelId.ACTIVE_ENERGY_L2, new UnsignedDoublewordElement(2135)), //
						new DummyRegisterElement(2137, 2165), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(2166))), //
				new FC3ReadRegistersTask(2235, Priority.HIGH, // //
						m(FeneconProPvMeter.ChannelId.ACTIVE_ENERGY_L3, new UnsignedDoublewordElement(2235)), //
						new DummyRegisterElement(2237, 2265), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(2266)))//

		); //
		return protocol;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {

		ACTIVE_ENERGY_L1(new Doc().unit(Unit.WATT_HOURS)), //
		ACTIVE_ENERGY_L2(new Doc().unit(Unit.WATT_HOURS)), //
		ACTIVE_ENERGY_L3(new Doc().unit(Unit.WATT_HOURS)), //

		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}
}
