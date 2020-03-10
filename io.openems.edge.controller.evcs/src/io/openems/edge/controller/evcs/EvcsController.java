package io.openems.edge.controller.evcs;

import java.io.IOException;
import java.time.Clock;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
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
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsController extends AbstractOpenemsComponent implements Controller, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EvcsController.class);
	private static final int CHARGE_POWER_BUFFER = 100;
	private static final double DEFAULT_UPPER_TARGET_DIFFERENCE_PERCENT = 0.10; // 10%

	private final ChargingLowerThanTargetHandler chargingLowerThanTargetHandler;

	private Config config;

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
				.unit(Unit.WATT).text("Minimum value for the force charge per Phase")),
		DEFAULT_CHARGE_MINPOWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text("Minimum value for a default charge")),
		PRIORITY(Doc.of(Priority.values()) //
				.initialValue(Priority.CAR) //
				.text("Which component getting preferred")), //
		ENABLED_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.text("Activates or deactivates the Charging")); //

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
		this.chargingLowerThanTargetHandler = new ChargingLowerThanTargetHandler(clock);
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

		this.channel(ChannelId.CHARGE_MODE).setNextValue(config.chargeMode());
		this.channel(ChannelId.PRIORITY).setNextValue(config.priority());
		this.channel(ChannelId.ENABLED_CHARGING).setNextValue(config.enabledCharging());
		this.channel(ChannelId.DEFAULT_CHARGE_MINPOWER).setNextValue(config.defaultChargeMinPower());
		this.channel(ChannelId.FORCE_CHARGE_MINPOWER).setNextValue(config.forceChargeMinPower());
		
		ManagedEvcs evcs = this.componentManager.getComponent(config.evcs_id());
		evcs.getMaximumPower().setNextValue(evcs.getMaximumHardwarePower().value().orElse(22800));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * If the EVCS is clustered the method will set the charge power request.
	 * Otherwise it will set directly the charge power limit.
	 */
	@Override
	public void run() throws OpenemsNamedException {
		ManagedEvcs evcs = this.componentManager.getComponent(config.evcs_id());
		SymmetricEss ess = this.componentManager.getComponent(config.ess_id());
		int maxHW = evcs.getMaximumHardwarePower().getNextValue().orElse(22800);
		if (maxHW != 0) {
			maxHW = (int) Math.ceil(maxHW / 100.0) * 100;
			if (config.defaultChargeMinPower() > maxHW) {
				configUpdate("defaultChargeMinPower", maxHW);
			}
			if (config.forceChargeMinPower() * evcs.getPhases().getNextValue().orElse(3) > maxHW) {
				configUpdate("forceChargeMinPower", maxHW / 3);
			}
		}

		evcs.setEnergyLimit().setNextWriteValue(config.energySessionLimit());

		/*
		 * Sets a fixed request of 0 if the Charger is not ready
		 */
		boolean isClustered = evcs.isClustered().getNextValue().orElse(false);
		if (isClustered) {

			Status status = evcs.status().getNextValue().asEnum();
			if (status == null) {
				evcs.status().setNextValue(Status.NOT_READY_FOR_CHARGING);
				status = Status.NOT_READY_FOR_CHARGING;
			}
			switch (status) {
			case ERROR:
			case STARTING:
			case UNDEFINED:
			case NOT_READY_FOR_CHARGING:
			case ENERGY_LIMIT_REACHED:
				evcs.setChargePowerRequest().setNextWriteValue(0);
				evcs.getMinimumPower().setNextValue(0);
				evcs.getMaximumPower().setNextValue(null);
				return;
			case CHARGING_REJECTED:
			case READY_FOR_CHARGING:
			case CHARGING_FINISHED:
				evcs.getMaximumPower().setNextValue(null);
			case CHARGING:
				break;
			}
		}

		/*
		 * Stop early if charging is disabled
		 */
		if (!config.enabledCharging()) {
			evcs.setChargePowerLimit().setNextWriteValue(0);
			return;
		}

		int nextChargePower = 0;
		int nextMinPower = 0;

		/*
		 * Calculates the next charging power depending on the charge mode
		 */
		switch (config.chargeMode()) {
		case EXCESS_POWER:
			/*
			 * Get the next charge power depending on the priority.
			 */
			switch (config.priority()) {
			case CAR:
				nextChargePower = this.calculateChargePowerFromExcessPower(evcs);
				break;

			case STORAGE:
				int storageSoc = this.sum.getEssSoc().value().orElse(0);
				if (storageSoc > 97) {
					nextChargePower = this.calculateChargePowerFromExcessPower(evcs);
				} else {
					nextChargePower = this.calculateExcessPowerAfterEss(evcs, ess);
				}
				break;
			}

			evcs.getMinimumPower().setNextValue(config.defaultChargeMinPower());
			nextMinPower = config.defaultChargeMinPower();
			break;

		case FORCE_CHARGE:
			evcs.getMinimumPower().setNextValue(0);
			nextChargePower = config.forceChargeMinPower() * evcs.getPhases().getNextValue().orElse(3);
			break;
		}

		if (nextChargePower < nextMinPower) {
			nextChargePower = nextMinPower;
		}

		/**
		 * Distribute next charge power on EVCS.
		 */
		if (isClustered) {
			// int chargePower = evcs.getChargePower().value().orElse(0);
			if (nextChargePower != 0) {

				int chargePower = evcs.getChargePower().value().orElse(0);

				// Check difference of the current charging and the previous charging target
				if (this.chargingLowerThanTargetHandler.isLower(evcs)) {
					if (chargePower <= evcs.getMinimumHardwarePower().getNextValue().orElse(1380)) {
						nextChargePower = 0;
					} else {
						nextChargePower = (chargePower + CHARGE_POWER_BUFFER);
						evcs.getMaximumPower().setNextValue(nextChargePower);
						if (this.config.debugMode()) {
							this.logInfo(this.log, "Set a lower charging target of " + nextChargePower + " W");
						}
					}
				} else {
					int currMax = evcs.getMaximumPower().value().orElse(0);

					if (chargePower > currMax * (1 + DEFAULT_UPPER_TARGET_DIFFERENCE_PERCENT)) {
						evcs.getMaximumPower().setNextValue(evcs.getMaximumHardwarePower().value().getOrError());
					}
				}
			}

			evcs.setChargePowerRequest().setNextWriteValue(nextChargePower);
		} else {
			evcs.setChargePowerLimit().setNextWriteValue(nextChargePower);
		}

		if (this.config.debugMode()) {
			this.logInfo(this.log, "Next charge power: " + nextChargePower + " W");
		}
	}

	/**
	 * Calculates the next charging power, depending on the current PV production
	 * and house consumption.
	 * 
	 * @param evcs Electric Vehicle Charging Station
	 * @return the available excess power for charging
	 * @throws OpenemsNamedException on error
	 */
	private int calculateChargePowerFromExcessPower(ManagedEvcs evcs) throws OpenemsNamedException {
		int nextChargePower;

		int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
		int essDischarge = this.sum.getEssActivePower().value().orElse(0);
		int essActivePowerDC = this.sum.getProductionDcActualPower().value().orElse(0);
		int evcsCharge = evcs.getChargePower().value().orElse(0);

		int excessPower = evcsCharge - buyFromGrid - (essDischarge - essActivePowerDC);

		nextChargePower = excessPower;

		Channel<Integer> minimumHardwarePowerChannel = evcs.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);
		if (nextChargePower < minimumHardwarePowerChannel.value().orElse(0)) { /* charging under 6A isn't possible */
			nextChargePower = 0;
		}
		return nextChargePower;
	}

	/**
	 * Calculates the next charging power from excess power after Ess charging.
	 * 
	 * @param evcs the ManagedEvcs
	 * @param ess  the ManagedSymmetricEss
	 * @return the available excess power for charging
	 */
	private int calculateExcessPowerAfterEss(ManagedEvcs evcs, SymmetricEss ess) {
		int maxEssCharge;
		if (ess instanceof ManagedSymmetricEss) {
			ManagedSymmetricEss e = (ManagedSymmetricEss) ess;
			Power power = ((ManagedSymmetricEss) ess).getPower();
			maxEssCharge = power.getMinPower(e, Phase.ALL, Pwr.ACTIVE);
			maxEssCharge = Math.abs(maxEssCharge);
		} else {
			maxEssCharge = ess.getMaxApparentPower().value().orElse(0);
		}
		int buyFromGrid = this.sum.getGridActivePower().value().orElse(0);
		int essActivePower = this.sum.getEssActivePower().value().orElse(0);
		int essActivePowerDC = this.sum.getProductionDcActualPower().value().orElse(0);
		int evcsCharge = evcs.getChargePower().value().orElse(0);
		int result = -buyFromGrid + evcsCharge - (maxEssCharge + (essActivePower - essActivePowerDC));
		result = result > 0 ? result : 0;

		return result;
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
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				Controller.getModbusSlaveNatureTable(accessMode));
	}

	public void configUpdate(String targetProperty, Object requiredValue) {
		// final String targetProperty = property + ".target";
		Configuration c;
		try {
			String pid = this.servicePid();
			if (pid.isEmpty()) {
				this.logInfo(log, "PID of " + this.id() + " is Empty");
				return;
			}
			c = cm.getConfiguration(pid, "?");
			Dictionary<String, Object> properties = c.getProperties();
			Object target = properties.get(targetProperty);
			String existingTarget = target.toString();
			if (!existingTarget.isEmpty()) {
				properties.put(targetProperty, requiredValue);
				c.update(properties);
			}
		} catch (IOException | SecurityException e) {
			this.logError(log, "ERROR: " + e.getMessage());
		}
	}
}
