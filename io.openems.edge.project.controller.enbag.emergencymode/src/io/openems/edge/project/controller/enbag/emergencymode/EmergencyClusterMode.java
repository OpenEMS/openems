package io.openems.edge.project.controller.enbag.emergencymode;

import java.util.ArrayList;

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
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.EmergencyClusterMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyClusterMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EmergencyClusterMode.class);

	@Reference
	protected ComponentManager componentManager;

	private ChannelAddress inputChannelAddress;
	private ChannelAddress outputChannelAddress;
//
	// defaults
	private boolean invertOutput = false;
	private boolean isSwitchedToOffGrid = true;
	private boolean primaryEssSwitch = false; // Q2
	private boolean backupEssSwitch = false; // Q1
	private boolean pvOnGridSwitch = false; // Q4
	private boolean pvOffGridSwitch = false; // Q3
	private int switchDealy = 10000; // 10 sec
	private int pvSwitchDelay = 10000; // 10 sec
	private int pvLimit = 100;
	private long lastPvOffGridDisconnected = 0L;
	private long waitOn = 0L;
	private long waitOff = 0L;

	private boolean allowChargeFromAC;
	private boolean gridFeedLimitation;
	private boolean isRemoteControlled;
	private boolean remoteStart;
	private int maxGridFeedPower;
	private int remoteActivePower;

	private PvState pvState = PvState.UNDEFINED;
	private EssState essState = EssState.UNDEFINED;
	private BatteryState batteryState = BatteryState.UNDEFINED;
	private SwitchState switchState = SwitchState.UNDEFINED;

	private ManagedSymmetricEss esss;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter gridMeter;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter pvMeter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricPvInverter pvInverter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent backupEssSwitchOutputComponent = null;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent backupEssSwitchInputComponent = null;
	private WriteChannel<Boolean> backupEssSwitchWrite = null;
	private Channel<Boolean> backupEssSwitchRead = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent primaryEssSwitchOutputComponent = null;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent primaryEssSwitchInputComponent = null;
	private WriteChannel<Boolean> primaryEssSwitchWrite = null;
	private Channel<Boolean> primaryEssSwitchRead = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent pvOffGridSwitchOutputComponent = null;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent pvOffGridSwitchInputComponent = null;
	private WriteChannel<Boolean> pvOffGridSwitchWrite = null;
	private Channel<Boolean> pvOffGridSwitchRead = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent pvOnGridSwitchOutputComponent = null;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private OpenemsComponent pvOnGridSwitchInputComponent = null;
	private WriteChannel<Boolean> pvOnGridSwitchWrite = null;
	private Channel<Boolean> pvOnGridSwitchRead = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss primaryEss;
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss backupEss;
	private Utils utils;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());

		ArrayList<Boolean> references = new ArrayList<Boolean>();
		// update filters
		try {

			// Solar Log
			references.add(OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "pvInverter",
					config.pv_inverter_id()));

			// meters
			references.add(OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "gridMeter",
					config.grid_meter_id()));
			references.add(
					OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "pvMeter", config.pv_meter_id()));

			// esss
			references.add(OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "primaryEss",
					config.primary_ess_id()));
			references.add(OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "backupEss",
					config.backup_ess_id()));

			if (!references.contains(false)) {
				// all update references passes
				return;
			}

			// wago
			// Q1
			ChannelAddress outputChannelAddress = ChannelAddress.fromString(config.Q1_outputChannelAddress());
			ChannelAddress inputChannelAddress = ChannelAddress.fromString(config.Q1_inputChannelAddress());
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "backupEssSwitchOutputComponent",
					outputChannelAddress.getComponentId())) {
				return;
			}
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "backupEssSwitchInputComponent",
					inputChannelAddress.getComponentId())) {
				return;
			}
			this.backupEssSwitchWrite = this.backupEssSwitchOutputComponent
					.channel(outputChannelAddress.getChannelId());
			this.backupEssSwitchRead = this.backupEssSwitchInputComponent.channel(inputChannelAddress.getChannelId());

			// Q2
			this.outputChannelAddress = ChannelAddress.fromString(config.Q2_outputChannelAddress());
			this.inputChannelAddress = ChannelAddress.fromString(config.Q2_inputChannelAddress());
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "primaryEssSwitchOutputComponent",
					outputChannelAddress.getComponentId())) {
				return;
			}
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "primaryEssSwitchInputComponent",
					inputChannelAddress.getComponentId())) {
				return;
			}
			this.primaryEssSwitchWrite = this.primaryEssSwitchOutputComponent
					.channel(outputChannelAddress.getChannelId());
			this.primaryEssSwitchRead = this.primaryEssSwitchInputComponent.channel(inputChannelAddress.getChannelId());

			// Q3
			this.outputChannelAddress = ChannelAddress.fromString(config.Q3_outputChannelAddress());
			this.inputChannelAddress = ChannelAddress.fromString(config.Q3_inputChannelAddress());
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "pvOffGridSwitchOutputComponent",
					outputChannelAddress.getComponentId())) {
				return;
			}
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "pvOffGridSwitchInputComponent",
					inputChannelAddress.getComponentId())) {
				return;
			}
			this.pvOffGridSwitchWrite = this.pvOffGridSwitchOutputComponent
					.channel(outputChannelAddress.getChannelId());
			this.pvOffGridSwitchRead = this.pvOffGridSwitchInputComponent.channel(inputChannelAddress.getChannelId());

			// Q4
			outputChannelAddress = ChannelAddress.fromString(config.Q4_outputChannelAddress());
			inputChannelAddress = ChannelAddress.fromString(config.Q4_inputChannelAddress());
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "pvOnGridSwitchOutputComponent",
					outputChannelAddress.getComponentId())) {
				return;
			}
			if (OpenemsComponent.updateReferenceFilter(this.cm, config.service_pid(), "pvOnGridSwitchInputComponent",
					inputChannelAddress.getComponentId())) {
				return;
			}
			this.pvOnGridSwitchWrite = this.pvOnGridSwitchOutputComponent.channel(outputChannelAddress.getChannelId());
			this.pvOnGridSwitchRead = this.pvOnGridSwitchInputComponent.channel(inputChannelAddress.getChannelId());

		} catch (OpenemsException e) {
			e.printStackTrace();
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		// make some preparations here
		this.maxGridFeedPower = config.maxGridFeedPower();
		this.allowChargeFromAC = config.allowChargeFromAC();
		this.remoteActivePower = config.remoteActivePower();
		this.gridFeedLimitation = config.gridFeedLimitation();
		this.remoteStart = config.remoteStart();
		this.isRemoteControlled = config.isRemoteControlled();
		// TODO dont use active
		// this.activeEss = this.primaryEss;
		this.utils = new Utils();
		this.utils.add(this.primaryEss);
		this.utils.add(this.backupEss);

		this.log.debug("EmergencyClusterMode bundle activated");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		if (this.remoteStart) {
			try {
				this.essState();
				this.switchState();
				this.pvInverter.getActivePowerLimit().setNextWriteValue(this.pvLimit);
				this.pvOnGridSwitchWrite.setNextWriteValue(this.pvOnGridSwitch);
				this.pvOffGridSwitchWrite.setNextWriteValue(this.pvOffGridSwitch);
				this.primaryEssSwitchWrite.setNextWriteValue(this.primaryEssSwitch);
				this.backupEssSwitchWrite.setNextWriteValue(this.backupEssSwitch);

				switch (this.essState) {
				case UNDEFINED:
					// TODO what if one of them off and the otheron grid
					if (this.utils.isBothEssOnGrid()) {
						this.essState = EssState.ON_GRID;
					} else {
						this.essState = EssState.OFF_GRID;
					}
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
								this.primaryEssSwitch = false;
								this.pvOffGridSwitch = false;
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
									this.primaryEssSwitch = true;
									this.backupEssSwitch = false;
									this.isSwitchedToOffGrid = false;
									this.pvOffGridSwitch = false;
									this.batteryState();
									// Active Ess is BackupEss
									switch (this.batteryState) {
									case BATTERY_HIGH:
										this.backupEssSwitch = true;
										break;
									case BATTERY_LOW:
										this.esss = this.primaryEss;
										this.primaryEssSwitch = false;
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
										this.primaryEssSwitch = true;
										this.backupEssSwitch = true;
									} else {
										// TODO wait for 10 seconds after switches are disconnected
									}
								} else {
									this.esss = this.backupEss;
									this.primaryEssSwitch = true;
									this.backupEssSwitch = false;
									// Condition Of BakcupEss Soc
									// TODO maybe we can check again switch status
									this.batteryState();
									switch (this.batteryState) {
									case BATTERY_HIGH:
										this.pvOffGridSwitch = false;
										break;
									case BATTERY_LOW:
										this.primaryEssSwitch = true;
										this.backupEssSwitch = false;
										if (allEssDisconnected()) {
											this.esss = this.primaryEss;
											this.primaryEssSwitch = false;
											this.backupEssSwitch = false;
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
						break;
					case UNDEFINED:
						break;
					}
					break;

				case ON_GRID:
					break;
				}

			} catch (OpenemsException e) {
				this.log.error("Error on reading remote Stop Element", e);
			}
		} else {
			this.log.info("Remote start is not available");
		}
	}

	/**
	 * Checks if both ESS devices are disconnected from grid -> primaryEssSwitch is
	 * NC so it must be true to be opened <-
	 * 
	 * @return boolean
	 */
	private boolean allEssDisconnected() throws InvalidValueException {
		if (!this.primaryEssSwitchRead.value().getOrError()) {
			return false;
		}
		if (this.backupEssSwitchRead.value().getOrError()) {
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
		if (!this.pvOnGridSwitchRead.value().getOrError()) {
			return false;
		}
		if (this.pvOffGridSwitchRead.value().getOrError()) {
			return false;
		}
		if (this.primaryEssSwitchRead.value().getOrError() && !this.backupEssSwitchRead.value().getOrError()) {
			return false;
		}
		return true;
	}

	/**
	 * Check if system is in Off Grid mode: -( Q4 false and Q3 true ) and ((Q2 true
	 * and Q1 true) or (Q2 false and Q1 false)) -
	 * 
	 * @return boolean
	 */
	private boolean isSwitchedToOffGrid() throws InvalidValueException {
		if (!this.primaryEssSwitchRead.value().getOrError() && !this.backupEssSwitchRead.value().getOrError()
				&& this.pvOffGridSwitchRead.value().getOrError() && !this.pvOnGridSwitchRead.value().getOrError()) {
			return true;
		} else if (this.primaryEssSwitchRead.value().getOrError() && this.backupEssSwitchRead.value().getOrError()
				&& this.pvOffGridSwitchRead.value().getOrError() && !this.pvOnGridSwitchRead.value().getOrError()) {
			return true;
		}
		return false;
	}

	private boolean isAllSwitchedToOpen() throws InvalidValueException {
		if (!allEssDisconnected()) {
			return false;
		}
		if (!pvOffGridSwitchRead.value().getOrError()) {
			return false;
		}
		if (pvOnGridSwitchRead.value().getOrError()) {
			return false;
		}
		return true;
	}

	private void essState() {
		if (this.utils.isBothEssOnGrid()) {
			this.essState = EssState.ON_GRID;
		} else {
			this.essState = EssState.OFF_GRID;
		}
	}

	private void switchState() throws InvalidValueException {
		this.isSwitchedToOffGrid();
		if (isSwitchedToOffGrid()) {
			this.switchState = SwitchState.SWITCH_AT_OFF_GRID;
		} else if (isSwitchedToOnGrid()) {
			this.switchState = SwitchState.SWITCH_AT_ON_GRID;
		} else if (isAllSwitchedToOpen()) {
			this.switchState = SwitchState.SWITCH_ALL_OPEN;
		} else {
			this.switchState = SwitchState.UNDEFINED;
		}
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

	private void setBiggerSocEss() {
		// TODO
	}
//
//	/**
//	 * Switch the output ON.
//	 * 
//	 * @throws OpenemsNamedException
//	 * @throws IllegalArgumentException
//	 */
//	private void on() throws IllegalArgumentException, OpenemsNamedException {
//		this.setOutput(true);
//	}
//
//	/**
//	 * Switch the output OFF.
//	 * 
//	 * @throws OpenemsNamedException
//	 * @throws IllegalArgumentException
//	 */
//	private void close() throws IllegalArgumentException, OpenemsNamedException {
//		this.setOutput(false);
//	}
//
//	// TODO I will re-write it again with considering for each channel
//	private void setOutput(boolean value) throws IllegalArgumentException, OpenemsNamedException {
//		try {
//			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(this.outputChannelAddress);
//			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
//			if (!currentValueOpt.isPresent() || currentValueOpt.get() != (value ^ this.invertOutput)) {
//				this.logInfo(this.log, "Set output [" + outputChannel.address() + "] "
//						+ (value ^ this.invertOutput ? "OPEN" : "CLOSE") + ".");
//				outputChannel.setNextWriteValue(value ^ invertOutput);
//			}
//		} catch (OpenemsException e) {
//			this.logError(this.log, "Unable to set output: [" + this.outputChannelAddress + "] " + e.getMessage());
//		}
//	}

}
