package io.openems.edge.meter.socomec.singlephase;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1_AND_INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SET_ZERO_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.chain;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.socomec.AbstractSocomecMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Socomec.Singlephase", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SocomecMeterSinglephaseImpl extends AbstractSocomecMeter implements SocomecMeterSinglephase, SocomecMeter,
		SinglePhaseMeter, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(SocomecMeterSinglephaseImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public SocomecMeterSinglephaseImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SocomecMeter.ChannelId.values(), //
				SocomecMeterSinglephase.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.identifySocomecMeter();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected void identifiedCountisE14() throws OpenemsException {
		this.modbusProtocol.addTask(//
				new FC3ReadRegistersTask(0xc558, Priority.HIGH, //
						m(new UnsignedDoublewordElement(0xc558)) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, chain(//
										SCALE_FACTOR_1, //
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L2, chain(//
										SCALE_FACTOR_1, //
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L3, chain(//
										SCALE_FACTOR_1, //
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L3))) //
								.build(), //
						new DummyRegisterElement(0xc55A, 0xc55D), //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(0xc55E)), //
						m(new UnsignedDoublewordElement(0xc560)) //
								.m(SymmetricMeter.ChannelId.CURRENT, SCALE_FACTOR_1) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L1,
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L1)) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L2,
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L2)) //
								.m(AsymmetricMeter.ChannelId.CURRENT_L3,
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L3)) //
								.build(), //
						new DummyRegisterElement(0xc562, 0xc567), //
						m(new SignedDoublewordElement(0xc568)) //
								.m(SymmetricMeter.ChannelId.ACTIVE_POWER,
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()))
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, chain(//
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()),
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, chain(//
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()),
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, chain(//
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()),
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L3))) //
								.build(), //
						m(new SignedDoublewordElement(0xc56A)) //
								.m(SymmetricMeter.ChannelId.REACTIVE_POWER,
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()))
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, chain(//
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()),
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L1))) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, chain(//
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()),
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L2))) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, chain(//
										SCALE_FACTOR_1_AND_INVERT_IF_TRUE(this.config.invert()),
										SET_ZERO_IF_TRUE(this.config.phase() != SinglePhase.L3))) //
								.build()));
		if (this.config.invert()) {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		} else {
			this.modbusProtocol.addTask(new FC3ReadRegistersTask(0xC702, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0xC702),
							SCALE_FACTOR_1), //
					new DummyRegisterElement(0xC704, 0xC707), //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0xC708),
							SCALE_FACTOR_1) //
			));
		}
	}

	@Override
	protected void identifiedCountisE23_E24_E27_E28() throws OpenemsException {
		this.thisIsNotASinglePhaseMeter();
	}

	@Override
	protected void identifiedCountisE34_E44() throws OpenemsException {
		this.thisIsNotASinglePhaseMeter();
	}

	@Override
	protected void identifiedDirisA10() throws OpenemsException {
		this.thisIsNotASinglePhaseMeter();
	}

	@Override
	protected void identifiedDirisA14() throws OpenemsException {
		this.thisIsNotASinglePhaseMeter();
	}

	@Override
	protected void identifiedDirisB30() throws OpenemsException {
		this.thisIsNotASinglePhaseMeter();
	}

	private void thisIsNotASinglePhaseMeter() {
		this.logError(this.log, "This is not a singlephase meter!");
		this.channel(SocomecMeterSinglephase.ChannelId.NOT_A_SINGLEPHASE_METER).setNextValue(true);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				SinglePhaseMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}

}
