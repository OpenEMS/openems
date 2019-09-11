package io.openems.edge.controller.evcs;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.osgi.service.cm.ConfigurationAdmin;
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
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.Evcs;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsController extends AbstractOpenemsComponent implements Controller, OpenemsComponent, ModbusSlave {

	private static final int STANDARD_HARDWARE_LIMIT = 22080;

	private static final int MAXIMUM_OUT_OF_RANGE_TRIES = 3;

	private static final int RUN_EVERY_SECONDS = 5;

	private final Logger log = LoggerFactory.getLogger(EvcsController.class);
	private final Clock clock;
	private Config config;
	private LocalDateTime lastRun = LocalDateTime.MIN;
	private int outOfRangeCounter = 0;
	private ManagedSymmetricEss ess;
	private ManagedEvcs evcs;
	private final static int CHECK_CHARGING_TARGET_DIFFERENCE_TIME = 30; // sec
	private final static int CHARGING_TARGET_MAX_DIFFERENCE = 500; // W
	private LocalDateTime lastChargingCheck = LocalDateTime.now();
	private int closestPowerToTarget = 0;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Sum sum;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_MODE(Doc.of(ChargeMode.values()) //
				.initialValue(ChargeMode.FORCE_CHARGE) //
				.text("Configured Charge-Mode")), //
		FORCE_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).text("Minimum value for the force charge")),
		DEFAULT_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Minimum value for a default charge")),
		PRIORITY(Doc.of(Priority.values()).initialValue(Priority.CAR).text("Which component getting preferred")), //
		ENABLED_CHARGING(Doc.of(OpenemsType.BOOLEAN).text("Aktivates or deaktivates the Charging")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public EvcsController() {
		this(Clock.systemDefaultZone());
	}

	protected EvcsController(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (config.forceChargeMinPower() < 0) {
			throw new OpenemsException("Force-Charge Min-Power [" + config.forceChargeMinPower() + "] must be >= 0");
		}

		if (config.defaultChargeMinPower() < 0) {
			throw new OpenemsException(
					"Default-Charge Min-Power [" + config.defaultChargeMinPower() + "] must be >= 0");
		}

		this.config = config;

		switch (config.chargeMode()) {
		case EXCESS_POWER:
			this.channel(ChannelId.DEFAULT_CHARGE_MINPOWER).setNextValue(config.defaultChargeMinPower());
			break;
		case FORCE_CHARGE:
			this.channel(ChannelId.FORCE_CHARGE_MINPOWER).setNextValue(config.forceChargeMinPower());
			break;
		}

		this.channel(ChannelId.CHARGE_MODE).setNextValue(config.chargeMode());
		this.channel(ChannelId.PRIORITY).setNextValue(config.priority());
		this.channel(ChannelId.ENABLED_CHARGING).setNextValue(config.enabledCharging());
		this.channel(ChannelId.DEFAULT_CHARGE_MINPOWER).setNextValue(config.defaultChargeMinPower());
		this.channel(ChannelId.FORCE_CHARGE_MINPOWER).setNextValue(config.forceChargeMinPower());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 *  If the evcs is clustered the method will set the charge power request.
	 *	Otherwise it will set directly the charge power.
	 */
	@Override
	public void run() throws OpenemsNamedException {
		this.evcs = this.componentManager.getComponent(config.evcs_id());
		this.ess = this.componentManager.getComponent(config.ess_id());

		Boolean isClusterd = Boolean.valueOf((this.evcs.isClustered().value().toString()));
		if (isClusterd != null && isClusterd) {

			// Sets a fixed request of 0 if the Charger can't charge
			if (evcs.status().value().isDefined()) {
				Status status = evcs.status().value().asEnum();
				switch (status) {
				case ERROR:
				case STARTING:
				case UNDEFINED:
				case NOT_READY_FOR_CHARGING:
					evcs.setChargePowerRequest().setNextWriteValue(0);
					this.evcs.getMinimumPower().setNextValue(0);
					return;
				case AUTHORIZATION_REJECTED:
				case READY_FOR_CHARGING:
				case CHARGING:
					break;
				}
			}
		}

		// Check if the maximum energy limit is reached, informs the user and sets the power request to 0 
		this.evcs.setEnergyLimit().setNextWriteValue(config.energySessionLimit());
		int limit = this.evcs.getEnergyLimit().value().orElse(0);
		if (this.evcs.getEnergySession().value().orElse(0) >= limit && limit != 0) {
			this.evcs.setDisplayText().setNextWriteValue("Limit of " + this.evcs.getEnergyLimit().value().orElse(0) + " reached");
			this.evcs.setChargePowerRequest().setNextWriteValue(0);
			return;
		}

		int nextChargePower = 0;
		int nextMinPower = 0;

		// Executes the following only if charging is enabled
		if (!config.enabledCharging()) {
			evcs.setChargePower().setNextWriteValue(0);
			return;
		}

		// Execute only every ... seconds
		if (this.lastRun.plusSeconds(RUN_EVERY_SECONDS).isAfter(LocalDateTime.now(this.clock))) {
			return;
		}

		// Calculates the next charging power depending on the charge mode
		switch (config.chargeMode()) {
		case EXCESS_POWER:
			switch (config.priority()) {
			case CAR:
				nextChargePower = nextChargePower_PvMinusConsumtion();
				break;

			case STORAGE:
				int storageSoc = this.sum.getEssSoc().value().orElse(0);

				if (storageSoc > 97) {
					nextChargePower = nextChargePower_PvMinusConsumtion();
				} else {
					int maxEssCharge = ess.getAllowedCharge().value().orElse(0);
					int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
					int essActivePower = this.sum.getEssActivePower().value().orElse(0);
					int evcsCharge = evcs.getChargePower().value().orElse(0);
					nextChargePower = -buyFromGrid + evcsCharge - (maxEssCharge + essActivePower);
					nextChargePower = nextChargePower > 0 ? nextChargePower : 0;
				}
				break;
			}
			this.evcs.getMinimumPower().setNextValue(config.defaultChargeMinPower());
			nextMinPower = config.defaultChargeMinPower();
			break;

		case FORCE_CHARGE:
			this.evcs.getMinimumPower().setNextValue(0);
			nextChargePower = config.forceChargeMinPower();
			break;
		}

		if (nextChargePower < nextMinPower) {
			nextChargePower = nextMinPower;
		}

		if (isClusterd != null && isClusterd) {
			int chargingPower = evcs.getChargePower().value().orElse(0);
			if (chargingPower != 0) {
				// Check difference of the current charging and the previous charging target
				this.outOfRangeCounter = chargingLowerThanTarget() ? this.outOfRangeCounter + 1 : 0;

				if (this.outOfRangeCounter >= MAXIMUM_OUT_OF_RANGE_TRIES) {
					nextChargePower = (this.closestPowerToTarget + 100) / this.evcs.getPhases().value().orElse(3);
					this.evcs.getMaximumPower().setNextValue(nextChargePower);
					this.logInfo(this.log, "Set a lower charging target of " + nextChargePower + " W");
					if (!chargingLowerThanTarget()) {
						this.outOfRangeCounter = 0;
						this.closestPowerToTarget = 0;
					}
				}

				// If a maximum charge power is defined.
				// The calculated charge power must be lower then this
				Optional<Integer> maxChargePower = evcs.getMaximumPower().value().asOptional();
				if (maxChargePower.isPresent()) {
					nextChargePower = maxChargePower.get() < nextChargePower ? maxChargePower.get() : nextChargePower;
				}
			}

			evcs.setChargePowerRequest().setNextWriteValue(nextChargePower);

		} else {
			evcs.setChargePower().setNextWriteValue(nextChargePower);
		}
		lastRun = LocalDateTime.now();
	}

	/**
	 * Check if the difference between the requested charging target and the real
	 * charging power is higher than the CHARGING_TARGET_MAX_DIFFERENCE
	 * 
	 * @return true if the difference is to high
	 */
	private boolean chargingLowerThanTarget() {

		int chargingPower = evcs.getChargePower().value().orElse(0);
		if (this.lastChargingCheck.plusSeconds(CHECK_CHARGING_TARGET_DIFFERENCE_TIME)
				.isBefore(LocalDateTime.now(this.clock))) {
			this.logInfo(this.log, "Charging Check for " + evcs.alias());
			int chargingPowerTarget = ((ManagedEvcs) evcs).getCurrChargingTarget().value().orElse(STANDARD_HARDWARE_LIMIT);
			this.logInfo(this.log, "Charging power: " + chargingPower);
			this.logInfo(this.log, "Charging target: " + chargingPowerTarget);
			if (chargingPowerTarget - chargingPower > CHARGING_TARGET_MAX_DIFFERENCE) {

				this.closestPowerToTarget = this.closestPowerToTarget > chargingPower ? this.closestPowerToTarget
						: chargingPower;
				lastChargingCheck = LocalDateTime.now();
				return true;
			}
			lastChargingCheck = LocalDateTime.now();
		}
		return false;
	}

	/**
	 * Calculates the next charging power, depending on the current PV production
	 * and house consumption
	 * 
	 * @return
	 * @throws OpenemsNamedException
	 */
	private int nextChargePower_PvMinusConsumtion() throws OpenemsNamedException {
		int nextChargePower;

		ManagedEvcs evcs = this.componentManager.getComponent(this.evcs.id());

		int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
		int essDischarge = this.sum.getEssActivePower().value().orElse(0);
		int evcsCharge = evcs.getChargePower().value().orElse(0);

		nextChargePower = evcsCharge - buyFromGrid - essDischarge;

		Channel<Integer> minChannel = evcs.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);
		if (nextChargePower < minChannel.value().orElse(0)) { /* charging under 6A isn't possible */
			nextChargePower = 0;
		}
		return nextChargePower;
	}

	@Override
	protected void logDebug(Logger log, String message) {
		super.logDebug(log, message);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(OpenemsComponent.getModbusSlaveNatureTable(accessMode));
	}
}
