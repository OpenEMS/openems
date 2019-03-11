package io.openems.edge.project.controller.enbag.emergencymode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

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

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;
import osgi.enroute.iot.gpio.util.Digital;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.EmergencyClusterMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyClusterMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EmergencyClusterMode.class);

	// defaults
	private boolean isSwitchedToOffGrid = true;
	private int switchDealy = 10000; // 10 sec
	private int pvSwitchDelay = 10000; // 10 sec
	private int pvLimit = 100;
	private long lastPvOffGridDisconnected = 0L;
	private long waitOn = 0L;
	private long waitOff = 0L;

	private Config config;

	private PvState pvState = PvState.UNDEFINED;
	private BatteryState batteryState = BatteryState.UNDEFINED;
	private SwitchState switchState = SwitchState.UNDEFINED;

	private ChannelAddress q1Ess1SupplyUps = null;
	private ChannelAddress q2Ess2SupplyUps = null;
	private ChannelAddress q3PvOffGrid = null;
	private ChannelAddress q4PvOnGrid = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter gridMeter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter pvMeter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricPvInverter pvInverter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess1;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess2;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());
		this.config = config;

		// Solar Log
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "pvInverter", config.pvInverter_id())) {
			return;
		}

		// Grid-Meter
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "gridMeter", config.gridMeter_id())) {
			return;
		}

		// PV-Meter
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "pvMeter", config.pvMeter_id())) {
			return;
		}

		// Ess1 (under switch Q1)
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess1", config.ess1_id())) {
			return;
		}

		// Ess2 (under switch Q2)
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess2", config.ess2_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		this.pvInverter.getActivePowerLimit().setNextWriteValue(this.pvLimit);
		this.pvOnGridSwitchWrite.setNextWriteValue(this.pvOnGridSwitch);
		this.pvOffGridSwitchWrite.setNextWriteValue(this.pvOffGridSwitch);
		this.ess2SwitchWrite.setNextWriteValue(this.ess2Switch);
		this.ess1SwitchWrite.setNextWriteValue(this.ess1Switch);

		switch (this.getGridMode()) {
		case UNDEFINED:
			// Wait...
			break;

		case OFF_GRID:
			switch (this.switchState) {
			case SWITCH_ALL_OPEN:
				if (this.waitOff <= System.currentTimeMillis()) {
					this.esss = this.primaryEss;
					this.batteryState();
					this.pvState();
					switch (this.batteryState) {
					case BATTERY_HIGH:
						this.setOutput(ess2SwitchOutput, Operation.CLOSE);
//								this.ess2Switch = false;
//								this.pvOffGridSwitch = false;
						this.isSwitchedToOffGrid = false;
						break;
					case BATTERY_LOW:
						switch (this.pvState) {
						// In case of Off grid - Switch All Open - Battery Low - Pv High
						// Compare Soc if backupEss has bigger, activate backup, deactivate primary
						// And close pvOffGridSwitch
						case PV_HIGH:
						case PV_LOW:
							// TODO SetBiggerSocEss
							this.esss = this.backupEss;
							this.setOutput(ess1SwitchOutput, Operation.OPEN);
							this.setOutput(ess2SwitchOutput, Operation.CLOSE);
//									this.ess2Switch = true;
//									this.ess1Switch = false;
							this.isSwitchedToOffGrid = false;
							this.pvOffGridSwitch = false;
							this.batteryState();
							// Active Ess is BackupEss
							switch (this.batteryState) {
							case BATTERY_HIGH:
								this.setOutput(ess1SwitchOutput, Operation.CLOSE);
//										this.ess1Switch = true;
								break;
							case BATTERY_LOW:
								this.esss = this.primaryEss;
								this.setOutput(ess2SwitchOutput, Operation.CLOSE);
//										this.ess2Switch = false;
								break;
							case BATTERY_OKAY:
								this.pvOffGridSwitch = false;
								break;
							case UNDEFINED:
								// TODO
								break;
							}
							break;
						case PV_OKAY:
							this.pvOffGridSwitch = true;
							this.isSwitchedToOffGrid = true;
							break;
						case UNDEFINED:
							// TOOD
							break;
						}
						break;
					case BATTERY_OKAY:
						this.pvOffGridSwitch = true;
						this.isSwitchedToOffGrid = true;
						break;
					case UNDEFINED:
						// TODO
						break;
					}
					// One more check for off grid mode
					if (this.pvOffGridSwitchRead.value().getOrError()) {
						this.isSwitchedToOffGrid = true;
					} else {
						this.isSwitchedToOffGrid = false;
					}
				}
				break;

			case SWITCH_AT_OFF_GRID:
				this.batteryState();
				switch (this.batteryState) {
				// Soc is bigger than 95%
				case BATTERY_HIGH:
					this.pvOffGridSwitch = false;
					break;
				// Soc is less than 5%
				case BATTERY_LOW:
					// TODO Setoutput to open All switches
					this.pvState();
					switch (this.pvState) {
					// Pv_High Is More Than 35kW
					case PV_HIGH:
						// TODO use setOutput
						this.pvInverter.getActivePowerLimit().setNextWriteValue(this.pvLimit);
						this.pvOffGridSwitch = false;
						this.lastPvOffGridDisconnected = System.currentTimeMillis();
						break;
					// Pv Power Is Less Than 3kW
					case PV_LOW:
						if (allEssDisconnected()) {
							if (this.waitOff + this.switchDealy <= System.currentTimeMillis()) {
								this.esss = this.backupEss;
								this.setOutput(ess1SwitchOutput, Operation.CLOSE);
								this.setOutput(ess2SwitchOutput, Operation.OPEN);
//										this.ess2Switch = true;
//										this.ess1Switch = true;
							} else {
								// TODO wait for 10 seconds after switches are disconnected
							}
						} else {
							this.esss = this.backupEss;
							this.setOutput(ess1SwitchOutput, Operation.OPEN);
							this.setOutput(ess2SwitchOutput, Operation.OPEN);
//									this.ess2Switch = true;
//									this.ess1Switch = false;
							// Condition Of BakcupEss Soc
							// TODO maybe we can check again switch status
							this.batteryState();
							switch (this.batteryState) {
							case BATTERY_HIGH:
								this.pvOffGridSwitch = false;
								break;
							case BATTERY_LOW:
								this.setOutput(ess1SwitchOutput, Operation.OPEN);
								this.setOutput(ess2SwitchOutput, Operation.OPEN);
//										this.ess2Switch = true;
//										this.ess1Switch = false;
								if (allEssDisconnected()) {
									this.esss = this.primaryEss;
									this.setOutput(ess1SwitchOutput, Operation.OPEN);
									this.setOutput(ess2SwitchOutput, Operation.CLOSE);
//											this.ess2Switch = false;
//											this.ess1Switch = false;
								} else {
									// TODO wait 10 sec until switch get closed
								}
								break;
							case BATTERY_OKAY:
								this.pvOffGridSwitch = true;
								break;
							case UNDEFINED:
								// TODO
								break;
							}
							this.pvOffGridSwitch = false;
							this.waitOff = System.currentTimeMillis();
						}
						break;
					// Pv Power Is between 3kW and 35kW
					case PV_OKAY:
						if (this.lastPvOffGridDisconnected + this.pvSwitchDelay <= System.currentTimeMillis()) {
							this.pvOffGridSwitch = true;
							this.isSwitchedToOffGrid = false;
						} else {
							this.pvOffGridSwitch = false;
							this.isSwitchedToOffGrid = false;
						}
						break;
					case UNDEFINED:
						// TODO
						break;
					}
					break;

				// Soc is between 5% and 95%
				case BATTERY_OKAY:
					this.pvState();
					switch (this.pvState) {
					case PV_HIGH:
						this.pvInverter.getActivePowerLimit().setNextWriteValue(this.pvLimit);
						this.pvOffGridSwitch = false;
						this.lastPvOffGridDisconnected = System.currentTimeMillis();
						break;
					case PV_LOW:
						// TODO
						this.pvOffGridSwitch = true;
						break;
					case PV_OKAY:
						if (this.lastPvOffGridDisconnected + this.pvSwitchDelay <= System.currentTimeMillis()) {
							this.pvOffGridSwitch = true;
						} else {
							this.pvOffGridSwitch = false;
						}
						break;
					case UNDEFINED:
						// TODO
						break;
					}
					break;
				case UNDEFINED:
					// TODO
					break;
				}
				break;

			case SWITCH_AT_ON_GRID:
				this.setOutput(ess1SwitchOutput, Operation.OPEN);
				this.setOutput(ess2SwitchOutput, Operation.OPEN);
//						this.ess2Switch = true;
//						this.ess1Switch = false;
				this.pvOffGridSwitch = false;
				this.pvOnGridSwitch = false;
				if (!allEssDisconnected() && !pvOffGridSwitchRead.value().getOrError()
						&& !pvOnGridSwitchRead.value().getOrError()) {
					// TODO wait 10 sec until switches get closed
				}
				break;
			case UNDEFINED:
				// TODO
				break;
			}
			break;

		case ON_GRID:
			switch (this.switchState) {
			case SWITCH_ALL_OPEN:
				if (this.waitOn + this.switchDealy <= System.currentTimeMillis()) {
					this.setOutput(ess2SwitchOutput, Operation.CLOSE);
//							this.ess2Switch = false;
					this.pvLimit = this.pvInverter.getActivePower().value().getOrError();
					this.pvOnGridSwitch = true;
					this.esss = null;
					this.isSwitchedToOffGrid = false;
				} else {
					// wait for 10 seconds after switches are disconnected
				}
				break;
			// Means Q1 or Q2 close, Q3 close and Q4 open
			case SWITCH_AT_OFF_GRID:
				this.setOutput(ess1SwitchOutput, Operation.OPEN);
				this.setOutput(ess2SwitchOutput, Operation.OPEN);
//						this.ess2Switch = true;
//						this.ess1Switch = false;
				this.pvOffGridSwitch = this.pvOnGridSwitch = false;
				this.waitOn = System.currentTimeMillis();
				break;
			case SWITCH_AT_ON_GRID:
				// system detects that grid is on, and it is switched to
				// On Grid
				try {
					int calculatedPower = this.utils.getActivePower()
							+ this.gridMeter.getActivePower().value().getOrError();
					int calculatedEssActivePower = calculatedPower;

					if (this.isRemoteControlled) {
						int maxPower = Math.abs(this.remoteActivePower);
						if (calculatedEssActivePower > maxPower) {
							calculatedEssActivePower = maxPower;
						} else if (calculatedEssActivePower < maxPower * -1) {
							calculatedEssActivePower = maxPower * -1;
						}
					}

					int essSoc = this.utils.getSoc();
					if (calculatedEssActivePower >= 0) {
						// discharge
						// adjust calculatedEssActivePower to max allowed discharge power
						if (this.utils.getAllowedDischarge() < calculatedEssActivePower) {
							calculatedEssActivePower = this.utils.getAllowedDischarge();
						}
					} else {
						// charge
						if (this.allowChargeFromAC) {
							// This is upper part of battery which is primarily used for charging during
							// peak PV production (after 11:00h)
							int reservedSoc = 50;
							if (LocalDateTime.now().getHour() <= 11 && essSoc > 100 - reservedSoc
									&& this.gridMeter.getActivePower().value().getOrError() < this.maxGridFeedPower) {
								// reduced charging formula – reduction based on current SOC and reservedSoc
								calculatedEssActivePower = calculatedEssActivePower / (reservedSoc * 2)
										* (reservedSoc - (essSoc - (100 - reservedSoc)));
							} else {
								// full charging formula – no restrictions except max charging power that
								// batteries can accept
								if (calculatedEssActivePower < this.utils.getAllowedCharge()) {
									calculatedEssActivePower = this.utils.getAllowedCharge();
								}
							}
						} else {
							// charging disallowed
							calculatedEssActivePower = 0;
						}
					}
					if (this.gridFeedLimitation) {
						// actual formula pvCounter.power + (calculatedEssActivePower-
						// cluster.allowedChargePower+ maxGridFeedPower+gridCounter.power)
						this.pvLimit = this.pvMeter.getActivePower().value().getOrError()
								+ (calculatedEssActivePower - this.utils.getAllowedCharge() + this.maxGridFeedPower
										+ this.gridMeter.getActivePower().value().getOrError());
						if (this.pvLimit < 0) {
							this.pvLimit = 0;
						}
					} else {
						this.pvLimit = this.pvInverter.getActivePower().value().getOrError();
					}
					this.utils.applyPower(calculatedEssActivePower, 0);
				} catch (InvalidValueException e) {
					this.log.error("An error occured on controll the storages!", e);
					this.pvLimit = 0;
					try {
						this.utils.applyPower(0, 0);
					} catch (InvalidValueException ee) {
						log.error("Failed to stop ess!");
					}
				}
				break;
			case UNDEFINED:
				// TODO
				break;
			}
			break;
		}
	}

	/**
	 * Checks if both ESS devices are disconnected from grid -> primaryEssSwitch is
	 * NC so it must be true to be opened <-
	 * 
	 * @return boolean
	 */
	private boolean allEssDisconnected() throws InvalidValueException {
		BooleanReadChannel ess1SwitchInput = this.ess1SwitchInputComponent.channel(this.ess1SwitchInput.getChannelId());
		Optional<Boolean> isEss1SwitchInput = ess1SwitchInput.value().asOptional();
		BooleanReadChannel ess2SwitchInput = this.ess2SwitchInputComponent.channel(this.ess2SwitchInput.getChannelId());
		Optional<Boolean> isEss2SwitchInput = ess2SwitchInput.value().asOptional();

		if (isEss1SwitchInput.isPresent() && !isEss1SwitchInput.get()) {
			return false;
		}

		if (isEss2SwitchInput.isPresent() && isEss2SwitchInput.get()) {
			return false;
		}
		return true;
	}

	/**
	 * Check if system is in On Grid mode: - and Q4 on and Q3 is off - (Q1 true , Q2
	 * false) or (Q1 false,Q2 false) or(Q1 , Q2 )
	 * 
	 * On the Other Hand if Q4 false, or Q3 true or (Q1 true and Q2 false) it is not
	 * On Grid
	 * 
	 * @return boolean
	 */
	private boolean isSwitchedToOnGrid() throws InvalidValueException {
//		BooleanReadChannel ess1SwitchInput = this.ess1SwitchInputComponent.channel(this.ess1SwitchInput.getChannelId());
//		Optional<Boolean> isEss1SwitchInput = ess1SwitchInput.value().asOptional();
//		BooleanReadChannel ess2SwitchInput = this.ess2SwitchInputComponent.channel(this.ess2SwitchInput.getChannelId());
//		Optional<Boolean> isEss2SwitchInput = ess2SwitchInput.value().asOptional();
//
//		if (!this.pvOnGridSwitchRead.value().getOrError()) {
//			return false;
//		}
//		if (this.pvOffGridSwitchRead.value().getOrError()) {
//			return false;
//		}
//		if (!isEss1SwitchInput.get() && isEss2SwitchInput.get()) {
//			return false;
//		}
		return true;
	}

	/**
	 * Check if system is in Off Grid mode: -( Q4 false and Q3 true ) and ((Q2 true
	 * and Q1 true) or (Q2 false and Q1 false)) -
	 * 
	 * @return boolean
	 */
	private boolean isSwitchedToOffGrid() throws InvalidValueException {
//		if (!this.ess2SwitchRead.value().getOrError() && !this.ess1SwitchInput
//				&& this.pvOffGridSwitchRead.value().getOrError() && !this.pvOnGridSwitchRead.value().getOrError()) {
//			return true;
//		} else if (this.ess2SwitchRead.value().getOrError() && this.ess1SwitchRead.value().getOrError()
//				&& this.pvOffGridSwitchRead.value().getOrError() && !this.pvOnGridSwitchRead.value().getOrError()) {
//			return true;
//		}
		return false;
	}

	private boolean isAllSwitchedToOpen() throws InvalidValueException {
		if (!allEssDisconnected()) {
			return false;
		}
//		if (!pvOffGridSwitchRead.value().getOrError()) {
//			return false;
//		}
//		if (pvOnGridSwitchRead.value().getOrError()) {
//			return false;
//		}
		return true;
	}

	private void essState() {
		if (this.utils.isBothEssOnGrid()) {
			this.essState = EssState.ON_GRID;
		} else {
			this.essState = EssState.OFF_GRID;
		}
	}

	/**
	 * Gets the Grid-Mode of both ESS.
	 * 
	 * @return the Grid-Mode
	 */
	private GridMode getGridMode() {
		GridMode ess1GridMode = this.ess1.getGridMode().value().asEnum();
		GridMode ess2GridMode = this.ess2.getGridMode().value().asEnum();
		if (ess1GridMode == GridMode.ON_GRID && ess2GridMode == GridMode.ON_GRID) {
			return GridMode.ON_GRID;
		} else if (ess1GridMode == GridMode.OFF_GRID && ess2GridMode == GridMode.OFF_GRID) {
			return GridMode.OFF_GRID;
		} else {
			return GridMode.UNDEFINED;
		}
	}

	/**
	 * Gets the state of the switches Q1 to Q4.
	 * 
	 * @return the Switch-State
	 */
	private SwitchState getSwitchState() {
		if (this.isQ4PvOnGridClosed() && !this.isQ3PvOffGridClosed()
				&& (this.isQ1Ess1SupplyUpsClosed() ^ this.isQ2Ess2SupplyUpsClosed())) {
			return SwitchState.SWITCHED_TO_ON_GRID;

		} else if (!this.isQ4PvOnGridClosed() && this.isQ3PvOffGridClosed()
				&& (this.isQ1Ess1SupplyUpsClosed() ^ this.isQ2Ess2SupplyUpsClosed())) {
			return SwitchState.SWITCHED_TO_OFF_GRID;

		} else {
			return SwitchState.UNDEFINED;
		}
	}

	private boolean isQ1Ess1SupplyUpsClosed() {
		BooleanWriteChannel q1Ess1SupplyUps = this.componentManager.getChannel(this.q1Ess1SupplyUps);
		return q1Ess1SupplyUps.value().orElse(false);
	}

	private boolean isQ2Ess2SupplyUpsClosed() {
		BooleanWriteChannel q2Ess2SupplyUps = this.componentManager.getChannel(this.q2Ess2SupplyUps);
		return !q2Ess2SupplyUps.value().orElse(false);
	}

	private boolean isQ3PvOffGridClosed() {
		BooleanWriteChannel q3PvOffGrid = this.componentManager.getChannel(this.q3PvOffGrid);
		return q3PvOffGrid.value().orElse(false);
	}

	private boolean isQ4PvOnGridClosed() {
		BooleanWriteChannel q4PvOnGrid = this.componentManager.getChannel(this.q4PvOnGrid);
		return q4PvOnGrid.value().orElse(false);
	}

	private void pvState() {
		if (this.pvInverter.getActivePower().value().get() <= 3000
				&& this.pvMeter.getActivePower().value().get() <= 3000) {
			this.pvState = PvState.PV_LOW;
		} else if (this.pvInverter.getActivePower().value().get() >= 35000
				&& this.pvMeter.getActivePower().value().get() >= 37000) {
			this.pvState = PvState.PV_HIGH;
		} else if (this.pvInverter.getActivePower().value().get() > 3000
				&& this.pvInverter.getActivePower().value().get() <= 35000
				&& this.pvMeter.getActivePower().value().get() > 3000
				&& this.pvMeter.getActivePower().value().get() <= 37000) {
			this.pvState = PvState.PV_OKAY;
		} else {
			this.pvState = PvState.UNDEFINED;
		}

	}

	// TODO Rename Battery State
	private void batteryState() {
		if (this.esss.getSoc().value().get() <= 5) {
			this.batteryState = BatteryState.BATTERY_LOW;
		} else if (5 < this.esss.getSoc().value().get() && this.esss.getSoc().value().get() < 95) {
			this.batteryState = BatteryState.BATTERY_OKAY;
		} else if (this.esss.getSoc().value().get() > 95) {
			this.batteryState = BatteryState.BATTERY_HIGH;
		} else {
			this.batteryState = BatteryState.UNDEFINED;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		STATE_INVERTER(new Doc() //
				.level(Level.INFO) //
				.text("Current State of Inverter") //
				.options(PvState.values()));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

//	private void setBiggerSocEss() {
//		// TODO
//	}
//

	private static enum Operation implements OptionsEnum {

		UNDEFINED(-1, "Undefined"), //

		CLOSE(0, "Close"), //

		OPEN(1, "Open");
		private final int value;
		private final String name;

		private Operation(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	/**
	 * Set Switch to Close or Open Operation.
	 * 
	 * @param Close --> Make line connection
	 * @param Open  --> Make line disconnection
	 * 
	 * @throws OpenemsNamedException
	 */
	private void setOutput(ChannelAddress channel, Operation operation) {
		Optional<Boolean> currentValueOpt = channel.;
		Boolean value;
		if (!currentValueOpt.isPresent()) {
			try {
				switch (operation) {
				case CLOSE:
					value = false;
					if (channel.channelId().equals(ess2SwitchWrite)) {
						channel.setNextWriteValue(value);
						log.info("Set output [" + channel.address() + "] " + (value ? "OPEN" : "CLOSE") + ".");
					} else {
						channel.setNextWriteValue(!value);
						log.info("Set output [" + channel.address() + "] " + (!value ? "OPEN" : "CLOSE") + ".");
					}
					break;

				case OPEN:
					value = true;
					if (channel.channelId().equals(ess2SwitchWrite)) {
						channel.setNextWriteValue(value);
						log.info("Set output [" + channel.address() + "] " + (value ? "OPEN" : "CLOSE") + ".");
					} else {
						channel.setNextWriteValue(!value);
						log.info("Set output [" + channel.address() + "] " + (!value ? "OPEN" : "CLOSE") + ".");
					}
					break;
				case UNDEFINED:
					break;
				}
			} catch (OpenemsException e) {
				this.logError(this.log, "Unable to set output: [" + channel.address() + "] " + e.getMessage());
			}
		}
	}
}
