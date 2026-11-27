package io.openems.edge.ess.saxpower.ess;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = io.openems.edge.ess.saxpower.ess.Config.class, factory = true)
@Component(//
		name = "Ess.SaxPower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class SaxPowerImpl extends AbstractOpenemsModbusComponent
		implements SaxPower, ManagedSinglePhaseEss, SinglePhaseEss, ManagedAsymmetricEss, AsymmetricEss,
		ManagedSymmetricEss, SymmetricEss, OpenemsComponent, ModbusComponent, ModbusSlave {

	private static final int MAX_APPARENT_POWER = 4600; // 230V * 20A

	private final Logger log = LoggerFactory.getLogger(SaxPowerImpl.class);

	@Reference
	private Power power;

	private Config config;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SaxPowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				SaxPower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		this.config = config;

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());

		final SinglePhase phase = config.phase();
		SinglePhaseEss.initializeCopyPhaseChannel(this, phase);

		setValue(this, SymmetricEss.ChannelId.MAX_APPARENT_POWER, MAX_APPARENT_POWER);
		setValue(this, SymmetricEss.ChannelId.GRID_MODE, GridMode.ON_GRID);
	}

	private int checkTimeout() {
		boolean correctTimeout = this.config.timeout() >= 1 && this.config.timeout() <= 300;
		if (!correctTimeout) {
			this.log.warn("Invalid timeout {} s, falling back to 60 s.", this.config.timeout());
		}

		return correctTimeout ? this.config.timeout() : 60;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(40029, Priority.HIGH,
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(40029)),
						new DummyRegisterElement(40030, 40048),
						m(SaxPower.ChannelId.POWER_TARGET, new SignedWordElement(40049)),
						m(SaxPower.ChannelId.TIMEOUT, new UnsignedWordElement(40050)),
						m(SaxPower.ChannelId.CONTROL_MODE, new UnsignedWordElement(40051)),
						new DummyRegisterElement(40052, 40052),
						m(SaxPower.ChannelId.REFERENCE_MAXIMUM_POWER, new UnsignedWordElement(40053)),
						new DummyRegisterElement(40054, 40096),
						m(SymmetricEss.ChannelId.CAPACITY, new SignedWordElement(40097)),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, new SignedWordElement(40098), INVERT),
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, new SignedWordElement(40099)),
						new DummyRegisterElement(40100, 40101), //
						m(SymmetricEss.ChannelId.SOC, new SignedWordElement(40102))),

				new FC16WriteRegistersTask(40049, m(SaxPower.ChannelId.POWER_TARGET, new SignedWordElement(40049)),
						m(SaxPower.ChannelId.TIMEOUT, new UnsignedWordElement(40050)),
						m(SaxPower.ChannelId.CONTROL_MODE, new UnsignedWordElement(40051))));
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsError.OpenemsNamedException {

		this.setControlMode(1);
		this.setTimeout(this.checkTimeout());

		final var maxPowerReferenceValue = this.getReferenceMaximumPower().get();
		if (maxPowerReferenceValue == null || maxPowerReferenceValue <= 0) {
			this.log.warn("Invalid maximum power reference value");
			return;
		}
		final int maxPowerReference = maxPowerReferenceValue;

		int percent = activePower * 10000 / maxPowerReference;
		var setPoint = (int) TypeUtils.fitWithin(-10000, 10000, percent);
		setPowerTarget(setPoint);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(OpenemsComponent.getModbusSlaveNatureTable(accessMode),
				SymmetricEss.getModbusSlaveNatureTable(accessMode),
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode),
				AsymmetricEss.getModbusSlaveNatureTable(accessMode),
				ManagedAsymmetricEss.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() + "|L:" + this.getActivePower().asString();
	}
}
