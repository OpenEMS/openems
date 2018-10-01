package io.openems.edge.sma;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "SMASunnyIsland6.0H", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)

public class SunnyIsland6Ess extends AbstractOpenemsModbusComponent
		implements SymmetricEss, ManagedSymmetricEss, OpenemsComponent {
	@Reference
	private Power power;

	private final static int UNIT_ID = 126;
	private String modbusBridgeId;

	@Reference
	protected ConfigurationAdmin cm;

	public SunnyIsland6Ess() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		this.setActivePowerChannel().setNextValue(activePower);
		this.setReactivePowerChannel().setNextValue(reactivePower);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, cm, config.modbus_id(),
				config.id());
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
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30201, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.SYTEM_STATE, new UnsignedDoublewordElement(30201)), //
						m(SunnyIsland6Ess.ChannelId.MAX_POWER, new UnsignedDoublewordElement(30203))), //
				new FC3ReadRegistersTask(30775, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30775)), //
						new DummyRegisterElement(30777, 30802), //
						m(SunnyIsland6Ess.ChannelId.FREQUENCY, new UnsignedDoublewordElement(30803)), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(30805),
								ElementToChannelConverter.INVERT)), //
				new FC3ReadRegistersTask(30843, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.BATTERY_CURRENT, new SignedDoublewordElement(30843)),
						m(SymmetricEss.ChannelId.SOC, new UnsignedDoublewordElement(30845)), //
						new DummyRegisterElement(30848, 30848), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_TEMPERATURE, new SignedDoublewordElement(30849)), //
						m(SunnyIsland6Ess.ChannelId.BATTERY_VOLTAGE, new UnsignedDoublewordElement(30851))), //
				new FC3ReadRegistersTask(40189, Priority.HIGH, //
						m(SunnyIsland6Ess.ChannelId.ALLOWED_CHARGE, new UnsignedDoublewordElement(40189),
								ElementToChannelConverter.INVERT), //
						m(SunnyIsland6Ess.ChannelId.ALLOWED_DISCHARGE, new UnsignedDoublewordElement(40191))), //
				new FC16WriteRegistersTask(40149, //
						m(SunnyIsland6Ess.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149)), //
						m(SunnyIsland6Ess.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						m(SunnyIsland6Ess.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))), //
				new FC16WriteRegistersTask(40705,
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_ON, new UnsignedDoublewordElement(40705)), //
						m(SunnyIsland6Ess.ChannelId.MIN_SOC_POWER_OFF, new UnsignedDoublewordElement(40707))), //
				new FC16WriteRegistersTask(41187,
						m(SunnyIsland6Ess.ChannelId.METER_SETTING, new UnsignedDoublewordElement(41187)))//
		);
		return protocol;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYTEM_STATE(new Doc()//
				.option(35, "Fehler").option(303, "Aus").option(307, "OK").option(455, "Warnung")), //
		MAX_POWER(new Doc().unit(Unit.WATT)), //
		FREQUENCY(new Doc().unit(Unit.MILLIHERTZ)), //
		BATTERY_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
		BATTERY_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
		BATTERY_TEMPERATURE(new Doc().unit(Unit.DEGREE_CELSIUS)), //
		ALLOWED_CHARGE(new Doc().unit(Unit.WATT)), //
		ALLOWED_DISCHARGE(new Doc().unit(Unit.WATT)), //
		SET_ACTIVE_POWER(new Doc().unit(Unit.WATT)), //
		SET_REACTIVE_POWER(new Doc().unit(Unit.VOLT_AMPERE)), //
		MIN_SOC_POWER_ON(new Doc()), //
		MIN_SOC_POWER_OFF(new Doc()), //
		SET_CONTROL_MODE(new Doc()//
				.option(802, "START")//
				.option(803, "STOP")), // S
		METER_SETTING(new Doc()//
				.option(3053, "SMA Energy Meter")//
				.option(3547, "Wechselrichter")),//
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
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	private IntegerWriteChannel setActivePowerChannel() {
		return this.channel(SunnyIsland6Ess.ChannelId.SET_ACTIVE_POWER);
	}

	private IntegerWriteChannel setReactivePowerChannel() {
		return this.channel(SunnyIsland6Ess.ChannelId.SET_ACTIVE_POWER);
	}

}
