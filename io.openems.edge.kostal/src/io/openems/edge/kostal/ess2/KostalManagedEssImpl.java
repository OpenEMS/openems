package io.openems.edge.kostal.ess2;

import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.google.common.collect.ImmutableMap;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.kostal.common.AbstractSunSpecEss;
import io.openems.edge.kostal.enums.ControlMode;
import io.openems.edge.kostal.ess2.charger.KostalDcCharger;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Kostal.Plenticore.plus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class KostalManagedEssImpl extends AbstractSunSpecEss
		implements
			KostalManagedEss,
			ManagedSymmetricEss,
			SymmetricEss,
			HybridEss,
			ModbusComponent,
			OpenemsComponent,
			EventHandler,
			ModbusSlave,
			TimedataProvider {

	private static final int READ_FROM_MODBUS_BLOCK = 1;
	private final List<KostalDcCharger> chargers = new ArrayList<>();

	// Hardware-Limits
	protected static final int HW_MAX_APPARENT_POWER = 5000;
	protected static final int HW_ALLOWED_CHARGE_POWER = -2500;
	protected static final int HW_ALLOWED_DISCHARGE_POWER = 2500;

	private Config config;
	private static int WATCHDOG_SECONDS = 30;

	private int cycleCounter = 60;
	private Integer lastSetPower;
	private Instant lastApplyPower = Instant.MIN;

	private ControlMode controlMode;
	private int minsoc = 5;

	@Reference
	private Power power;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap
			.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_113, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_120, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_123, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_203, Priority.LOW) //
			.put(DefaultSunSpecModel.S_802, Priority.LOW) //
			.build();
	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;
	private int targetDiff = 200;
	private boolean managed = false;

	public KostalManagedEssImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				KostalManagedEss.ChannelId.values());

		this.addStaticModbusTasks(this.getModbusProtocol());
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
		this.config = config;
		this.controlMode = config.controlMode();
		this.minsoc = config.minsoc();
		WATCHDOG_SECONDS = config.watchdog();

		try {
			this._setMaxApparentPower(HW_MAX_APPARENT_POWER);
			this._setMaxDischargePower(HW_ALLOWED_DISCHARGE_POWER);
			this._setMaxChargePower(HW_ALLOWED_CHARGE_POWER * -1);

			if (isManaged()) {
				System.out
						.println("--> initially setting charge power to zero");
				try {
					// initialize
					this.applyPower(0, 0);
				} catch (OpenemsNamedException e) {
					e.printStackTrace();
				}
			}

		} catch (OpenemsNamedException e) {
			// e.printStackTrace();
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void addCharger(KostalDcCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(KostalDcCharger charger) {
		this.chargers.remove(charger);
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	public void applyPower(int activePowerWanted, int reactivePowerWanted)
			throws OpenemsNamedException {

		// Using separate channel for the demanded charge/discharge power
		this._setChargePowerWanted(activePowerWanted);

		// Read-only mode -> switch to max. self consumption automatic
		if (isManaged()) {
			// allow minimum writes if values does not change (zero)
			Instant now = Instant.now();
			if (this.lastSetPower != null
					&& Duration.between(this.lastApplyPower, now)
							.getSeconds() < WATCHDOG_SECONDS) {
				// no need to apply to new set-point
				// TODO remove syso
				if (!this.managed) {
					System.out.println("skipped, delayed");
					return;
				} else {
					if (activePowerWanted == this.lastSetPower) {
						System.out.println("skipped, equals");
						return;
					}
				}
			}

			this.cycleCounter++;
			if ((this.cycleCounter >= 10)
					|| (activePowerWanted != lastSetPower)) {
				this.cycleCounter = 0;

				try {
					// DebugSetActivePower
					IntegerReadChannel setActivePowerEqualsChannel = this
							.channel(
									ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER);
					int powerValue = setActivePowerEqualsChannel.value().get();

					// TODO testing - realization depends on controller order
					// (by scheduler)
					// System.out.println("old value: " + powerValue);

				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Kostal is fine by writing one register with signed value
				IntegerWriteChannel setActivePowerChannel = this
						.channel(KostalManagedEss.ChannelId.SET_ACTIVE_POWER);
				if (!isSmartModeOptimized(activePowerWanted)) {
					setActivePowerChannel.setNextWriteValue(activePowerWanted);

					lastSetPower = activePowerWanted;
					this.lastApplyPower = Instant.now();
					managed = true;
				} else {
					managed = false;
					this.lastApplyPower.plusSeconds(60);
				}

				// TODO remove...
				System.out
						.println("--> activePowerWanted: " + activePowerWanted);
			}
		} else {
			lastSetPower = null;
			managed = false;
		}
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol
	 *            the {@link ModbusProtocol}
	 * @throws OpenemsException
	 *             on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol)
			throws OpenemsException {

		// TODO check if required - overwriting sunspec model?
		protocol.addTask(//
				new FC3ReadRegistersTask(531, Priority.LOW, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER,
								new UnsignedWordElement(531)))); //

		// protocol.addTask(//
		// new FC3ReadRegistersTask(582, Priority.HIGH, //
		// m(SymmetricEss.ChannelId.ACTIVE_POWER,
		// new SignedWordElement(582))));

		// TODO double check - enable widget charge/discharge power?
		protocol.addTask(//
				new FC3ReadRegistersTask(582, Priority.HIGH, //
						m(HybridEss.ChannelId.DC_DISCHARGE_POWER,
								new SignedWordElement(582))));

		protocol.addTask(//
				new FC3ReadRegistersTask(1038, Priority.HIGH, //
						m(KostalManagedEss.ChannelId.MAX_CHARGE_POWER,
								new FloatDoublewordElement(1038)
										.wordOrder(LSWMSW)), //
						m(KostalManagedEss.ChannelId.MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(1040)
										.wordOrder(LSWMSW)))); //

		protocol.addTask(//
				new FC3ReadRegistersTask(1034, Priority.LOW, m(
						KostalManagedEss.ChannelId.CHARGE_POWER,
						new FloatDoublewordElement(1034).wordOrder(LSWMSW)))); //

		protocol.addTask(//
				new FC3ReadRegistersTask(1046, Priority.LOW,
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
								new FloatDoublewordElement(1046)
										.wordOrder(LSWMSW)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
								new FloatDoublewordElement(1048)
										.wordOrder(LSWMSW)))); //

		protocol.addTask(//
				new FC16WriteRegistersTask(1034, //
						m(KostalManagedEss.ChannelId.SET_ACTIVE_POWER,
								new FloatDoublewordElement(1034)
										.wordOrder(LSWMSW)) //
				// protocol.addTask(//
				// new FC16WriteRegistersTask(1034, //
				// m(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS,
				// new FloatDoublewordElement(1034)
				// .wordOrder(LSWMSW)) //
				));
	}

	// /**
	// * Actual power from inverter comes from house consumption + battery
	// * inverter power (*-1). Aktuelle Erzeugung durch den Hybrid-WR ist der
	// * aktuelle Verbrauch + Batterie-Ladung/Entladung *-1
	// *
	// */
	// public void _setInverterActivePower() {
	//
	// int acPower = this.getAcPower().orElse(0);
	// // int acPowerScale = this.getAcPowerScale().orElse(0);
	// // double value = acPower * Math.pow(10, acPowerScale);
	// double value = acPower;
	//
	// this._setActivePower((int) value);
	//
	// }

	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103

		// Example:
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);

		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.CONSUMPTION_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);

		// DefaultSunSpecModel.S103.W);

		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);

		// AC-Output from the Inverter. Could be the combination from PV +
		// battery
		/*
		 * this.mapFirstPointToChannel(// SymmetricEss.ChannelId.ACTIVE_POWER,
		 * // ElementToChannelConverter.DIRECT_1_TO_1, //
		 * DefaultSunSpecModel.S103.W);
		 */

		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.SOC, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S802.SO_C);

		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.CAPACITY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S802.W_H_RTG);

		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.V_AR);

		// TODO substracted in UI widget by DC_DISCHARGE_POWER - otherwise
		// S802.W
		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.DCW);
		// TODO check, what's correct (DCW is DC side, W is AC side - Inverter)
		// DefaultSunSpecModel.S103.W/S103.DCW);

		// this.setLimits();
	}

	@Override
	public String debugLog() {
		if (this.config.debugMode()) {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString() //
					+ "|Allowed Charge Power:"
					+ this.channel(
							ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER)
							.value().asStringWithoutUnit()
					+ ";" + "|Allowed DisCharge Power:"
					+ this.channel(
							ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER)
							.value().asStringWithoutUnit()
					+ "|MaxChargePower "
					+ this.channel(KostalManagedEss.ChannelId.MAX_CHARGE_POWER)
							.value().asStringWithoutUnit() //
					+ "|MaxDischargePower "
					+ this.channel(
							KostalManagedEss.ChannelId.MAX_DISCHARGE_POWER)
							.value().asStringWithoutUnit() //
					+ "|ChargePower "
					+ this.channel(KostalManagedEss.ChannelId.CHARGE_POWER)
							.value().asString() //
					+ "|" + this.getGridModeChannel().value().asOptionString() //
			// + "|Feed-In:" //
			// + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value()
			// .asStringWithoutUnit() //
			;
		} else {
			return "SoC:" + this.getSoc().asString() + "|L:"
					+ this.getActivePower().asString();
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		// super.handleEvent(event);

		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE :
				// this._setMyActivePower();
				break;
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS :
				this.setLimits();
				break;
		}
	}

	private void setLimits() {
		int maxDischargePower = getMaxDischargePower().orElse(0);
		int maxChargePower = getMaxChargePower().orElse(0) * -1;

		this._setAllowedDischargePower(maxDischargePower);
		this._setAllowedChargePower(maxChargePower);

		try {
			int soc = getSoc().get();
			if (soc == 100) {
				// maximum state of charge
				this._setAllowedChargePower(0);
			}
			if (soc <= this.minsoc) {
				// minimum state of charge
				this._setAllowedDischargePower(0);
			}
		} catch (NullPointerException e) {
			// e.printStackTrace();
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				HybridEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable
						.of(KostalManagedEssImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		//
		return 1;
	}

	@Override
	public boolean isManaged() {
		return (this.config.enabled() && !this.config.readOnlyMode()
				&& this.controlMode == ControlMode.REMOTE);
	}

	@Override
	public Integer getSurplusPower() {
		// return this.surplusFeedInHandler.run(this.chargers, this.config,
		// this.componentManager);
		return null;
	}

	private boolean isSmartModeOptimized(int activePower) {
		if (this.controlMode == ControlMode.SMART) {
			return false;
		}
		try {
			int currentActivePowerAbs = Math.abs(this.getActivePower().get());
			int activePowerAbs = Math.abs(activePower);

			if (Math.abs(
					currentActivePowerAbs - activePowerAbs) > this.targetDiff
					&& !this.managed) {
				// internal control is not sufficient
				return true;
			}
		} catch (NullPointerException e) {
			// e.printStackTrace();
		}
		return false;
	}

}
