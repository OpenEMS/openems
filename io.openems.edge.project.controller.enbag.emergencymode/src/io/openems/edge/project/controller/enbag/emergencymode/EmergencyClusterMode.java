package io.openems.edge.project.controller.enbag.emergencymode;

import java.time.LocalDateTime;

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
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.EmergencyClusterMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyClusterMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EmergencyClusterMode.class);

	// defaults
	private int switchDealy = 10000; // 10 sec
	private int pvSwitchDelay = 10000; // 10 sec
	private int pvLimit = 100;
	private long lastPvOffGridDisconnected = 0L;
	private long waitOn = 0L;
	private long waitOff = 0L;

	private Config config;

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

		// TODO still requires?
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
		switch (this.getGridMode()) {
		case UNDEFINED:
			// Wait...
			break;

		case OFF_GRID:
			switch (getSwitchState()) {
			case UNDEFINED:
				this.setOutput(this.componentManager.getChannel(q4PvOnGrid), Operation.OPEN);
				if (this.waitOff <= System.currentTimeMillis()) {
					switch (batteryState(BatteryEnum.ESS2SOC)) {
					case BATTERY_HIGH:
						this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.OPEN);
						this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.CLOSE);
						this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
						break;
					case BATTERY_LOW:
						switch (pvState()) {
						// In case of (Off grid - Battery Low - Pv High/Low)
						// Compare Soc if backupEss has bigger, activate backup, deactivate primary
						// And close pvOffGridSwitch
						case PV_HIGH:
						case PV_LOW:
							// Active Ess is BackupEss
							switch (batteryState(BatteryEnum.ESS1SOC)) {
							case BATTERY_HIGH:
								this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.CLOSE);
								this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.OPEN);
								this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
								break;
							case BATTERY_LOW:
							case BATTERY_OKAY:
								this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.OPEN);
								this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.CLOSE);
								this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.CLOSE);
								break;
							case UNDEFINED:
								break;
							}
							break;
						case PV_OKAY:
							this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.OPEN);
							this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.CLOSE);
							this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.CLOSE);
							break;
						case UNDEFINED:
							break;
						}
						break;
					case BATTERY_OKAY:
						switch (pvState()) {
						case PV_HIGH:
							this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.OPEN);
							this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.CLOSE);
							this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
							break;
						case PV_LOW:
						case PV_OKAY:
							this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.OPEN);
							this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.CLOSE);
							this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.CLOSE);
							break;
						case UNDEFINED:
							break;
						}
						break;
					case UNDEFINED:
						break;
					}
				}
				break;

			case SWITCHED_TO_OFF_GRID:
				if (isQ2Ess2SupplyUpsClosed()) {
					switch (batteryState(BatteryEnum.ESS2SOC)) {
					// Soc is bigger than 95%
					case BATTERY_HIGH:
						this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
						break;
					// Soc is less than 5%
					case BATTERY_LOW:
						switch (pvState()) {
						// Pv_High Is More Than 35kW
						case PV_HIGH:
							// TODO ask logic to Nils again
							this.pvInverter.getActivePowerLimit().setNextWriteValue(this.pvLimit);
							this.lastPvOffGridDisconnected = System.currentTimeMillis();
							switch (batteryState(BatteryEnum.ESS1SOC)) {
							case BATTERY_OKAY:
							case BATTERY_HIGH:
								this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
								this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.OPEN);
								this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.CLOSE);
								break;
							case BATTERY_LOW:
								this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
								break;
							case UNDEFINED:
								break;
							}
							break;
						// Pv Power Is Less Than 3kW
						case PV_LOW:
//							TODO switch closing time ?
//							if (this.waitOff + this.switchDealy <= System.currentTimeMillis()) {
							switch (batteryState(BatteryEnum.ESS1SOC)) {
							case BATTERY_OKAY:
							case BATTERY_HIGH:
								this.setOutput(this.componentManager.getChannel(q1Ess1SupplyUps), Operation.CLOSE);
								this.setOutput(this.componentManager.getChannel(q2Ess2SupplyUps), Operation.OPEN);
								break;
							case BATTERY_LOW:
								// Keep on staying at OffGrid with ess2
								break;
							case UNDEFINED:
								break;
							}
							this.waitOff = System.currentTimeMillis();
//							} else {
//								 TODO wait for 10 seconds after switches are disconnected
//							}
							break;
						// Pv Power Is between 3kW and 35kW
						case PV_OKAY:
							// TODO check it !!
						case UNDEFINED:
							break;
						}
						break;

					// Soc is between 5% and 95%
					case BATTERY_OKAY:
						switch (pvState()) {
						case PV_HIGH:
							this.pvInverter.getActivePowerLimit().setNextWriteValue(this.pvLimit);
							this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
							this.lastPvOffGridDisconnected = System.currentTimeMillis();
							break;
						case PV_LOW:
						case PV_OKAY:
							if (this.lastPvOffGridDisconnected + this.pvSwitchDelay <= System.currentTimeMillis()) {
								this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.CLOSE);
							} else {
								this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
							}
							break;
						case UNDEFINED:
							break;
						}
						break;
					case UNDEFINED:
						break;
					}
				} else {
					// TODO if q1 close ?
				}
				break;
			case SWITCHED_TO_ON_GRID:
				if (isQ2Ess2SupplyUpsClosed()) {
					this.setOutput(this.componentManager.getChannel(q4PvOnGrid), Operation.OPEN);
					this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.CLOSE);
					// TODO this.getBiggerSoc();

				} else if (isQ1Ess1SupplyUpsClosed()) {
					this.setOutput(this.componentManager.getChannel(q4PvOnGrid), Operation.OPEN);
					this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.CLOSE);
				}
				break;
			}
			break;

		// Its not important at all if we hav epv low, load goinf to supplied by grid
		case ON_GRID:
			switch (getSwitchState()) {
			case SWITCHED_TO_OFF_GRID:
				this.setOutput(this.componentManager.getChannel(q3PvOffGrid), Operation.OPEN);
				this.setOutput(this.componentManager.getChannel(q4PvOnGrid), Operation.CLOSE);
				this.waitOn = System.currentTimeMillis();
				break;
			case SWITCHED_TO_ON_GRID:
//				 system detects that grid is on, and it is switched to
//				 On Grid
				try {
					if (isQ2Ess2SupplyUpsClosed()) {
						this.setPvLimitation(ess2);
					} else {
						this.setPvLimitation(ess1);
					}

				} catch (InvalidValueException e) {
					this.log.error("An error occured on controll the storages!", e);
					this.pvLimit = 0;
//						TODO  ? this.utils.applyPower(0, 0);
				}
				break;
			case UNDEFINED:
				break;
			}
			break;
		}

	}

	private void setPvLimitation(ManagedSymmetricEss ess) {
		Channel<Integer> essActivePowerChannel = ess.getActivePower();
		int essActivePower = essActivePowerChannel.value().orElse(0);
		int calculatedEssActivePower = essActivePower + this.gridMeter.getActivePower().value().orElse(0);

		Channel<Integer> essSocChannel = ess.getSoc();
		int essSoc = essSocChannel.value().orElse(0);
		int maxPower = ess.getPower().getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		int minPower = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		if (calculatedEssActivePower >= 0) {
			// discharge
			// adjust calculatedEssActivePower to max allowed discharge power
			if (maxPower < calculatedEssActivePower) {
				calculatedEssActivePower = minPower;
			}
		} else {
			// charge
			// This is upper part of battery which is primarily used for charging during
			// peak PV production (after 11:00h)
			int reservedSoc = 50;
			if (LocalDateTime.now().getHour() <= 11 && essSoc > 100 - reservedSoc
					&& this.gridMeter.getActivePower().value().orElse(0) < config.maxGridFeedPower()) {
				// reduced charging formula – reduction based on current SOC and reservedSoc
				calculatedEssActivePower = calculatedEssActivePower / (reservedSoc * 2)
						* (reservedSoc - (essSoc - (100 - reservedSoc)));
			} else {
				// full charging formula – no restrictions except max charging power that
				// batteries can accept
				if (calculatedEssActivePower < maxPower) {
					calculatedEssActivePower = maxPower;
				}
			}
		}
		if (config.gridFeedLimitation()) {
			// actual formula pvCounter.power + (calculatedEssActivePower-
			// cluster.allowedChargePower+ maxGridFeedPower+gridCounter.power)
			this.pvLimit = this.pvMeter.getActivePower().value().orElse(0) + (calculatedEssActivePower - maxPower
					+ config.maxGridFeedPower() + this.gridMeter.getActivePower().value().orElse(0));
			if (this.pvLimit < 0) {
				this.pvLimit = 0;
			}
		} else {
			this.pvLimit = this.pvInverter.getActivePower().value().orElse(0);
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
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private SwitchState getSwitchState() throws IllegalArgumentException, OpenemsNamedException {
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

	private boolean isQ1Ess1SupplyUpsClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q1Ess1SupplyUps = this.componentManager.getChannel(this.q1Ess1SupplyUps);
		return q1Ess1SupplyUps.value().orElse(false);
	}

	private boolean isQ2Ess2SupplyUpsClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q2Ess2SupplyUps = this.componentManager.getChannel(this.q2Ess2SupplyUps);
		return !q2Ess2SupplyUps.value().orElse(false);
	}

	private boolean isQ3PvOffGridClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q3PvOffGrid = this.componentManager.getChannel(this.q3PvOffGrid);
		return q3PvOffGrid.value().orElse(false);
	}

	private boolean isQ4PvOnGridClosed() throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel q4PvOnGrid = this.componentManager.getChannel(this.q4PvOnGrid);
		return q4PvOnGrid.value().orElse(false);
	}

	private PvState pvState() {
		Channel<Integer> pvInverterChannel = this.pvInverter.getActivePower();
		int pvInverter = pvInverterChannel.value().orElse(0);
		Channel<Integer> pvMeterChannel = this.pvMeter.getActivePower();
		int pvMeter = pvMeterChannel.value().orElse(0);

		if (pvInverter < 3000 && pvMeter < 3000) {
			return PvState.PV_LOW;
		} else if (pvInverter >= 35000 && pvMeter >= 37000) {
			return PvState.PV_HIGH;
		} else if (pvInverter > 3000 && pvInverter <= 35000 && pvMeter > 3000 && pvMeter <= 37000) {
			return PvState.PV_OKAY;
		}
		return PvState.UNDEFINED;
	}

	private static enum BatteryEnum implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //

		ESS1SOC(0, "Close"), //

		ESS2SOC(1, "Open");
		private final int value;
		private final String name;

		private BatteryEnum(int value, String name) {
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

	private BatteryEnum getBiggerSoc() {
		Channel<Integer> ess1SocChannel = this.ess1.getSoc();
		int ess1Soc = ess1SocChannel.value().orElse(0);
		Channel<Integer> ess2SocChannel = this.ess2.getSoc();
		int ess2Soc = ess2SocChannel.value().orElse(0);
		if (ess1Soc > ess2Soc) {
			return BatteryEnum.ESS1SOC;
		} else {
			return BatteryEnum.ESS1SOC;
		}
	}

	/**
	 * Gets the soc of the Ess1, Ess2.
	 * 
	 * @return the Battery-Soc
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private BatteryState batteryState(BatteryEnum ess) {
		Channel<Integer> ess1SocChannel = this.ess1.getSoc();
		int ess1Soc = ess1SocChannel.value().orElse(0);
		Channel<Integer> ess2SocChannel = this.ess2.getSoc();
		int ess2Soc = ess2SocChannel.value().orElse(0);

		switch (ess) {
		case ESS1SOC:
			if (ess1Soc <= 5) {
				return BatteryState.BATTERY_LOW;
			} else if (5 < ess1Soc && ess1Soc < 95) {
				return BatteryState.BATTERY_OKAY;
			} else if (ess1Soc > 95) {
				return BatteryState.BATTERY_HIGH;
			}
			break;
		case ESS2SOC:
			if (ess2Soc <= 5) {
				return BatteryState.BATTERY_LOW;
			} else if (5 < ess2Soc && ess2Soc < 95) {
				return BatteryState.BATTERY_OKAY;
			} else if (ess2Soc > 95) {
				return BatteryState.BATTERY_HIGH;
			}
			break;
		case UNDEFINED:
			return BatteryState.UNDEFINED;
		}

		return BatteryState.UNDEFINED;
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
	private void setOutput(BooleanWriteChannel channel, Operation operation) {
		try {
			Boolean value;
			switch (operation) {
			case CLOSE:
				value = false;
				if (channel.address().equals(this.q2Ess2SupplyUps)) {
					channel.setNextWriteValue(value);
					log.info("Set output [" + channel.address() + "] " + (value ? "OPEN" : "CLOSE") + ".");
				} else {
					channel.setNextWriteValue(!value);
					log.info("Set output [" + channel.address() + "] " + (!value ? "OPEN" : "CLOSE") + ".");
				}
				break;

			case OPEN:
				value = true;
				if (channel.address().equals(this.q2Ess2SupplyUps)) {
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
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
