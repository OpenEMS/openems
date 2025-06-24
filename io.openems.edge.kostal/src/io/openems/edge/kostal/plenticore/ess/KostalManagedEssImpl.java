package io.openems.edge.kostal.plenticore.ess;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.kostal.plenticore.enums.ControlMode;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kostal.Plenticore.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class KostalManagedEssImpl extends AbstractOpenemsModbusComponent implements KostalManagedEss,
		ManagedSymmetricEss, SymmetricEss, ModbusComponent, TimedataProvider, EventHandler, OpenemsComponent {

	private static final Logger log = LoggerFactory.getLogger(KostalManagedEss.class);

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	/**
	 * Sets the Modbus bridge service reference. This method is used to reference
	 * the Modbus bridge component.
	 *
	 * @param modbus the Modbus bridge instance
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
	private int tolerance = 20;
	private int watchdog = 30;

	// is DC power for consistency
	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	/**
	 * Constructor for KostalManagedESSImpl. Initializes the component with default
	 * channels.
	 */
	public KostalManagedEssImpl() {
		super(OpenemsComponent.ChannelId.values(), ModbusComponent.ChannelId.values(), SymmetricEss.ChannelId.values(),
				HybridEss.ChannelId.values(), ManagedSymmetricEss.ChannelId.values(),
				KostalManagedEss.ChannelId.values());
	}

	/**
	 * Activates the component and initializes the configuration. This method is
	 * called when the component is activated.
	 *
	 * @param context the component context
	 * @param config  the configuration settings
	 * @throws OpenemsException if there are activation issues
	 */
	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this._setGridMode(GridMode.ON_GRID);
		this._setCapacity(config.capacity());
		this.controlMode = config.controlMode();
		this.minsoc = config.minsoc();
		this.watchdog = config.watchdog();
		this.tolerance = config.tolerance();
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
	 * @param activePower   the desired active power
	 * @param reactivePower the desired reactive power
	 * @throws OpenemsNamedException if there are issues applying power
	 */
	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		// Using separate channel for the demanded charge/discharge power
		this._setChargePowerWanted(activePower);

		// managed or internal mode -> switch to max. self consumption automatic
		// (no writes to channel)
		if (this.isManaged() && this.controlMode != ControlMode.INTERNAL) {
			// allow minimum writes if values not set (zero or null)
			Instant now = Instant.now();
			// allows moderate differences
			if (this.lastSetPower != null && activePower == 0 && this.lastSetPower == activePower
					&& (this.lastSetPower - this.tolerance >= activePower
							&& this.lastSetPower + this.tolerance <= activePower)
					&& Duration.between(this.lastApplyPower, now).getSeconds() < this.watchdog) {

				// no need to apply to new set-point
				log.debug("skipped - wait for expiring watchdog (zero)");
				return;
			}

			// allow minimum writes if values are maximized (smart control)
			if (this.lastSetPower != null && this.controlMode == ControlMode.SMART && (this.lastSetPower == activePower
					// allows little differences
					|| (this.lastSetPower - this.tolerance >= activePower
							&& this.lastSetPower + this.tolerance <= activePower)
					// at limits
					|| (activePower == this.getMaxChargePower().get()
							|| Math.abs(activePower) == this.getMaxDischargePower().get()) //
					// in tolerance around zero
					|| (Math.abs(activePower) < this.tolerance))
					&& Duration.between(this.lastApplyPower, now).getSeconds() < this.watchdog) {

				// no need to apply to new set-point
				log.debug("skipped - wait for expiring watchdog (tolerance)");
				return;
			}

			// write to channel if necessary (expired/changed)
			if (this.lastSetPower == null || activePower != this.lastSetPower
					|| Duration.between(this.lastApplyPower, now).getSeconds() >= this.watchdog) {

				// in tolerance around zero
				if (Math.abs(activePower) < this.tolerance) {
					activePower = 0;
				}

				// Kostal is fine by writing one register with signed value
				IntegerWriteChannel setActivePowerChannel = this.channel(KostalManagedEss.ChannelId.SET_ACTIVE_POWER);
				setActivePowerChannel.setNextWriteValue(activePower);

				this.lastSetPower = activePower;
				this.lastApplyPower = Instant.now();

				log.debug("--> activePowerWanted: " + activePower);
			}
		} else {
			this.lastSetPower = null;
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
				new FC3ReadRegistersTask(56, Priority.LOW,
						m(KostalManagedEss.ChannelId.INVERTER_STATE,
								new UnsignedDoublewordElement(56).wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(104, Priority.LOW,
						m(KostalManagedEss.ChannelId.ENERGY_MANAGER_MODE,
								new UnsignedDoublewordElement(104).wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(152, Priority.HIGH,
						m(KostalManagedEss.ChannelId.FREQUENCY, new FloatDoublewordElement(152).wordOrder(LSWMSW)),
						new DummyRegisterElement(154, 157), //
						m(KostalManagedEss.ChannelId.GRID_VOLTAGE_L1,
								new FloatDoublewordElement(158).wordOrder(LSWMSW)),
						new DummyRegisterElement(160, 163), //
						m(KostalManagedEss.ChannelId.GRID_VOLTAGE_L2,
								new FloatDoublewordElement(164).wordOrder(LSWMSW)),
						new DummyRegisterElement(166, 169), //
						m(KostalManagedEss.ChannelId.GRID_VOLTAGE_L3,
								new FloatDoublewordElement(170).wordOrder(LSWMSW)),
						new DummyRegisterElement(172, 173), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(174).wordOrder(LSWMSW)),
						new DummyRegisterElement(176, 189), //
						m(KostalManagedEss.ChannelId.BATTERY_CURRENT,
								new FloatDoublewordElement(190).wordOrder(LSWMSW)),
						new DummyRegisterElement(192, 209), //
						m(SymmetricEss.ChannelId.SOC, new FloatDoublewordElement(210).wordOrder(LSWMSW)),
						new DummyRegisterElement(212, 213), //
						m(KostalManagedEss.ChannelId.BATTERY_TEMPERATURE,
								new FloatDoublewordElement(214).wordOrder(LSWMSW)),
						m(KostalManagedEss.ChannelId.BATTERY_VOLTAGE, new FloatDoublewordElement(216).wordOrder(LSWMSW),
								SCALE_FACTOR_3)),
				new FC3ReadRegistersTask(531, Priority.LOW,
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(531)),
						new DummyRegisterElement(532, 581), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(582))),
				new FC3ReadRegistersTask(1034, Priority.LOW,
						m(KostalManagedEss.ChannelId.CHARGE_POWER, new FloatDoublewordElement(1034).wordOrder(LSWMSW)),
						new DummyRegisterElement(1036, 1037), //
						m(KostalManagedEss.ChannelId.MAX_CHARGE_POWER,
								new FloatDoublewordElement(1038).wordOrder(LSWMSW)),
						m(KostalManagedEss.ChannelId.MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(1040).wordOrder(LSWMSW))),

				new FC16WriteRegistersTask(1034, m(KostalManagedEss.ChannelId.SET_ACTIVE_POWER,
						new FloatDoublewordElement(1034).wordOrder(LSWMSW))));

	}

	/**
	 * Provides a debug log message summarizing the current state.
	 *
	 * @return the debug log message
	 */
	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed Charge Power:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit()
				+ "|Allowed Discharge Power:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asStringWithoutUnit()
				+ "|MaxChargePower:"
				+ this.channel(KostalManagedEss.ChannelId.MAX_CHARGE_POWER).value().asStringWithoutUnit()
				+ "|MaxDischargePower:"
				+ this.channel(KostalManagedEss.ChannelId.MAX_DISCHARGE_POWER).value().asStringWithoutUnit()
				+ "|ChargePower:" + this.channel(KostalManagedEss.ChannelId.CHARGE_POWER).value().asString();
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
	 * @param event the event to handle
	 */
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_CONTROLLERS:
			log.debug("== update values topic cycle before controllers ==");
			this.setLimits();
			break;
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			log.debug("== update values topic cycle before process image ==");
			this.calculateEnergy();
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
		log.debug("--> set limits: " + maxDischargePower + " / " + maxChargePower);
	}

	private void calculateEnergy() {
		// Calculate AC Energy
		var activePower = this.getActivePowerChannel().getNextValue().get();
		if (activePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else {
			log.debug("valid active power for calculation of energy");
			if (activePower > 0) {
				// Discharge
				this.calculateAcChargeEnergy.update(0);
				this.calculateAcDischargeEnergy.update(activePower);
			} else {
				// Charge
				this.calculateAcChargeEnergy.update(activePower * -1);
				this.calculateAcDischargeEnergy.update(0);
			}
		}
	}
}
