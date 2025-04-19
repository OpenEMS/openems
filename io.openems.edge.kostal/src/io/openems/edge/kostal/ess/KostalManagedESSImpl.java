package io.openems.edge.kostal.ess;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

import java.time.Duration;
import java.time.Instant;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.kostal.enums.ControlMode;
import io.openems.edge.kostal.ess2.KostalManagedEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Kostal.Plenticore", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS})
public class KostalManagedESSImpl extends AbstractOpenemsModbusComponent
		implements
			KostalManagedESS,
			ManagedSymmetricEss,
			SymmetricEss,
			ModbusComponent,
			TimedataProvider,
			EventHandler,
			OpenemsComponent {

	private static final Integer diff = 20;

	private static int WATCHDOG_SECONDS = 30;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	/**
	 * Sets the Modbus bridge service reference. This method is used to
	 * reference the Modbus bridge component.
	 *
	 * @param modbus
	 *            the Modbus bridge instance
	 */
	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	private Instant lastApplyPower = Instant.MIN;
	private Integer lastSetPower = 0;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timeData;

	private ControlMode controlMode;
	private int minsoc = 5;

	/**
	 * Constructor for KostalManagedESSImpl. Initializes the component with
	 * default channels.
	 */
	public KostalManagedESSImpl() {
		super(OpenemsComponent.ChannelId.values(),
				ModbusComponent.ChannelId.values(),
				SymmetricEss.ChannelId.values(), HybridEss.ChannelId.values(),
				ManagedSymmetricEss.ChannelId.values(),
				KostalManagedESS.ChannelId.values());
	}

	/**
	 * Activates the component and initializes the configuration. This method is
	 * called when the component is activated.
	 *
	 * @param context
	 *            the component context
	 * @param config
	 *            the configuration settings
	 * @throws OpenemsException
	 *             if there are activation issues
	 */
	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}

		this._setGridMode(GridMode.ON_GRID);
		this._setCapacity(config.capacity());
		this.controlMode = config.controlMode();
		this.minsoc = config.minsoc();
		WATCHDOG_SECONDS = config.watchdog();

		// // Initialize controller reference filter
		// try {
		// if (OpenemsComponent.updateReferenceFilter(this.cm,
		// this.servicePid(), "ctrl", config.ctrl_id())) {
		// return;
		// }
		// } catch (Exception e) {
		// this.ctrl = null;
		// this.mode = -1;
		// // Ignore exception for failed reference
		// }
	}

	/**
	 * Deactivates the component. Resets internal states and references.
	 */
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Applies the desired active and reactive power to the system.
	 *
	 * @param activePower
	 *            the desired active power
	 * @param reactivePower
	 *            the desired reactive power
	 * @throws OpenemsNamedException
	 *             if there are issues applying power
	 */
	@Override
	public void applyPower(int activePower, int reactivePower)
			throws OpenemsNamedException {
		// Using separate channel for the demanded charge/discharge power
		this._setChargePowerWanted(activePower);

		// managed or internal mode -> switch to max. self consumption automatic
		// (no writes to channel)
		if (isManaged() && this.controlMode != ControlMode.INTERNAL) {
			// allow minimum writes if values not set (zero or null)
			Instant now = Instant.now();
			if (this.lastSetPower != null && activePower == 0
					&& lastSetPower == activePower
					// TODO testing - allows moderate differences
					&& (lastSetPower - diff >= activePower
							&& lastSetPower + diff <= activePower)
					&& Duration.between(this.lastApplyPower, now)
							.getSeconds() < WATCHDOG_SECONDS) {

				// no need to apply to new set-point
				if (config.debugMode())
					System.out.println(
							"skipped - wait for expiring watchdog (zero)");
				return;
			}

			// allow minimum writes if values are maximized (smart control)
			if (this.lastSetPower != null
					&& this.controlMode == ControlMode.SMART
					&& lastSetPower == activePower
					// TODO testing - allows little differences
					&& (lastSetPower - diff >= activePower
							&& lastSetPower + diff <= activePower)
					&& (activePower == this.getMaxChargePower().get()
							|| Math.abs(activePower) == this
									.getMaxDischargePower().get())
					&& Duration.between(this.lastApplyPower, now)
							.getSeconds() < WATCHDOG_SECONDS) {

				// no need to apply to new set-point
				if (config.debugMode())
					System.out.println(
							"skipped - wait for expiring watchdog (maximum)");
				return;
			}

			// write to channel if necessary (expired/changed)
			if (lastSetPower == null || activePower != lastSetPower
					|| Duration.between(this.lastApplyPower, now)
							.getSeconds() >= WATCHDOG_SECONDS) {

				// Kostal is fine by writing one register with signed value
				IntegerWriteChannel setActivePowerChannel = this
						.channel(KostalManagedEss.ChannelId.SET_ACTIVE_POWER);
				setActivePowerChannel.setNextWriteValue(activePower);

				lastSetPower = activePower;
				this.lastApplyPower = Instant.now();

				if (config.debugMode())
					System.out.println("--> activePowerWanted: " + activePower);
			}
		} else {
			lastSetPower = null;
		}
	}

	/**
	 * Defines the Modbus protocol for this component.
	 *
	 * @return the ModbusProtocol instance
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(14, Priority.LOW,
						m(KostalManagedESS.ChannelId.SERIAL_NUMBER,
								new StringWordElement(14, 8))),
				new FC3ReadRegistersTask(56, Priority.LOW,
						m(KostalManagedESS.ChannelId.INVERTER_STATE,
								new UnsignedDoublewordElement(56)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(104, Priority.LOW,
						m(KostalManagedESS.ChannelId.ENERGY_MANAGER_MODE,
								new UnsignedDoublewordElement(104)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(174, Priority.HIGH,
						m(SymmetricEss.ChannelId.REACTIVE_POWER,
								new FloatDoublewordElement(174)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(152, Priority.HIGH,
						m(KostalManagedESS.ChannelId.FREQUENCY,
								new FloatDoublewordElement(152)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(158, Priority.HIGH,
						m(KostalManagedESS.ChannelId.GRID_VOLTAGE_L1,
								new FloatDoublewordElement(158)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(164, Priority.HIGH,
						m(KostalManagedESS.ChannelId.GRID_VOLTAGE_L2,
								new FloatDoublewordElement(164)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(170, Priority.HIGH,
						m(KostalManagedESS.ChannelId.GRID_VOLTAGE_L3,
								new FloatDoublewordElement(170)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(190, Priority.HIGH,
						m(KostalManagedESS.ChannelId.BATTERY_CURRENT,
								new FloatDoublewordElement(190)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(210, Priority.HIGH,
						m(SymmetricEss.ChannelId.SOC,
								new FloatDoublewordElement(210)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(214, Priority.HIGH,
						m(KostalManagedESS.ChannelId.BATTERY_TEMPERATURE,
								new FloatDoublewordElement(214)
										.wordOrder(LSWMSW)),
						m(KostalManagedESS.ChannelId.BATTERY_VOLTAGE,
								new FloatDoublewordElement(216)
										.wordOrder(LSWMSW),
								SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(531, Priority.LOW,
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER,
								new UnsignedWordElement(531))),
				new FC3ReadRegistersTask(582, Priority.HIGH,
						m(SymmetricEss.ChannelId.ACTIVE_POWER,
								new SignedWordElement(582))),
				new FC3ReadRegistersTask(1034, Priority.LOW,
						m(KostalManagedESS.ChannelId.CHARGE_POWER,
								new FloatDoublewordElement(1034)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(1038, Priority.HIGH,
						m(KostalManagedESS.ChannelId.MAX_CHARGE_POWER,
								new FloatDoublewordElement(1038)
										.wordOrder(LSWMSW)),
						m(KostalManagedESS.ChannelId.MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(1040)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(1046, Priority.LOW,
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
								new FloatDoublewordElement(1046)
										.wordOrder(LSWMSW)),
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
								new FloatDoublewordElement(1048)
										.wordOrder(LSWMSW))),
				new FC16WriteRegistersTask(1034, m(
						KostalManagedESS.ChannelId.SET_ACTIVE_POWER,
						new FloatDoublewordElement(1034).wordOrder(LSWMSW))));
	}

	/**
	 * Provides a debug log message summarizing the current state.
	 *
	 * @return the debug log message
	 */
	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() + "|L:"
				+ this.getActivePower().asString() + "|Allowed Charge Power:"
				+ this.channel(
						ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER)
						.value().asStringWithoutUnit()
				+ "|Allowed Discharge Power:"
				+ this.channel(
						ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER)
						.value().asStringWithoutUnit()
				+ "|MaxChargePower:"
				+ this.channel(KostalManagedESS.ChannelId.MAX_CHARGE_POWER)
						.value().asStringWithoutUnit()
				+ "|MaxDischargePower:"
				+ this.channel(KostalManagedESS.ChannelId.MAX_DISCHARGE_POWER)
						.value().asStringWithoutUnit()
				+ "|ChargePower:"
				+ this.channel(KostalManagedESS.ChannelId.CHARGE_POWER).value()
						.asString();
	}

	/**
	 * Gets the current power instance.
	 *
	 * @return the current Power instance
	 */
	@Override
	public Power getPower() {
		return this.power;
	}

	/**
	 * Gets the power precision for this component.
	 *
	 * @return the power precision
	 */
	@Override
	public int getPowerPrecision() {
		return 1;
	}

	/**
	 * Gets the timedata provider.
	 *
	 * @return the Timedata instance
	 */
	@Override
	public Timedata getTimedata() {
		return this.timeData;
	}

	/**
	 * Handles system events and reacts to specific topics.
	 *
	 * @param event
	 *            the event to handle
	 */
	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE :
				if (config.debugMode())
					System.out
							.print("== update values before process image ==");
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
				if (config.debugMode())
					System.out.print("== update values after process image ==");
				break;
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS :
				if (config.debugMode())
					System.out.print(
							"== update values topic cycle before controllers ==");
				this.setLimits();
				break;
		}
	}

	/**
	 * Checks if this component is in managed mode.
	 *
	 * @return true if in managed mode; false otherwise
	 */
	@Override
	public boolean isManaged() {
		// && this.controlMode != ControlMode.INTERNAL
		return (this.config.enabled() && !this.config.readOnlyMode());
	}

	/**
	 * Sets power limits based on system state and configuration.
	 */
	private void setLimits() {
		int maxDischargePower = getMaxDischargePower().orElse(0);
		int maxChargePower = getMaxChargePower().orElse(0) * -1;

		this._setAllowedDischargePower(maxDischargePower);
		this._setAllowedChargePower(maxChargePower);

		try {
			int soc = getSoc().get();
			if (soc == 100) {
				this._setAllowedChargePower(0);
			}
			if (soc <= this.minsoc) {
				this._setAllowedDischargePower(0);
			}
		} catch (NullPointerException e) {
			// Handle potential null values gracefully
		}
		if (config.debugMode()) {
			System.out.println("--> set limits: " + maxDischargePower + " / "
					+ maxChargePower);
		}
	}
}
