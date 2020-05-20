package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ed.data.BatteryData;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.kaco.blueplanet.hybrid10.BpConstants;
import io.openems.edge.kaco.blueplanet.hybrid10.ErrorChannelId;
import io.openems.edge.kaco.blueplanet.hybrid10.GlobalUtils;
import io.openems.edge.kaco.blueplanet.hybrid10.core.BpCore;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Kaco.BlueplanetHybrid10.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class BpEssImpl extends AbstractOpenemsComponent
		implements BpEss, ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private final int WATCHDOG_SECONDS = 8;

	private final Logger log = LoggerFactory.getLogger(BpEssImpl.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BpCore core;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	private Config config;

	public BpEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ErrorChannelId.values(), //
				BpEss.ChannelId.values() //
		);
		this.getMaxApparentPower().setNextValue(BpConstants.MAX_APPARENT_POWER);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "core", config.core_id())) {
			return;
		}

		this.config = config;
		this.getCapacity().setNextValue(config.capacity());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			this.calculateEnergy();
			break;
		}
	}

	private void updateChannels() {
		Integer soc = null;
		Integer activePower = null;
		Integer reactivePower = null;
		GridMode gridMode = GridMode.UNDEFINED;
		Float bmsVoltage = null;
		Float riso = null;
		Integer inverterStatus = null;
		Integer batteryStatus = null;

		if (!this.core.isConnected()) {
			this.logWarn(this.log, "Core is not connected!");

		} else {
			BatteryData battery = this.core.getBatteryData();
			Status status = this.core.getStatusData();
			InverterData inverter = this.core.getInverterData();

			if (battery != null) {
				soc = Math.round(battery.getSOE());
				activePower = GlobalUtils.roundToPowerPrecision(battery.getPower() - inverter.getPvPower()) * -1; // invert
				bmsVoltage = battery.getBmsVoltage();
			}

			if (status != null) {
				switch (status.getInverterStatus()) {
				case 12:
					gridMode = GridMode.OFF_GRID;
					break;
				case 13:
				case 14:
					gridMode = GridMode.ON_GRID;
					break;

				}

				inverterStatus = status.getInverterStatus();
				batteryStatus = status.getBatteryStatus();

				// Set error channels
				List<String> errors = status.getErrors().getErrorCodes();
				for (ErrorChannelId channelId : ErrorChannelId.values()) {
					this.channel(channelId).setNextValue(errors.contains(channelId.getErrorCode()));
				}
			}

			if (inverter != null) {
				reactivePower = (GlobalUtils.roundToPowerPrecision(inverter.getReactivPower(0))
						+ GlobalUtils.roundToPowerPrecision(inverter.getReactivPower(1))
						+ GlobalUtils.roundToPowerPrecision(inverter.getReactivPower(2))) * -1;
				riso = inverter.getRIso();
			}
		}

		this.getSoc().setNextValue(soc);
		this.getActivePower().setNextValue(activePower);
		this.getReactivePower().setNextValue(reactivePower);
		this.getGridMode().setNextValue(gridMode);

		if (soc == null || soc >= 99) {
			this.getAllowedCharge().setNextValue(0);
		} else {
			this.getAllowedCharge().setNextValue(BpConstants.MAX_APPARENT_POWER * -1);
		}
		if (soc == null || soc <= 0) {
			this.getAllowedDischarge().setNextValue(0);
		} else {
			this.getAllowedDischarge().setNextValue(BpConstants.MAX_APPARENT_POWER);
		}

		this.channel(BpEss.ChannelId.BMS_VOLTAGE).setNextValue(bmsVoltage);
		this.channel(BpEss.ChannelId.RISO).setNextValue(riso);
		this.channel(BpEss.ChannelId.INVERTER_STATUS).setNextValue(inverterStatus);
		this.channel(BpEss.ChannelId.BATTERY_STATUS).setNextValue(batteryStatus);

		// Surplus Feed-In Channel
		this.channel(BpEss.ChannelId.SURPLUS_FEED_IN).setNextValue(this.calculateSurplusFeedIn());
	}

	/**
	 * Calculates the Surplus-Feed-In Power, i.e. the power that should be forced to
	 * be 'discharged' and fed to grid.
	 * 
	 * <p>
	 * This is called by {@link #updateChannels()} once per Cycle to make sure it
	 * does not change within one Cycle. The value is used in
	 * {@link #getStaticConstraints()}.
	 * 
	 * @return the surplus feed-in power or null for no force feed-in.
	 */
	private Integer calculateSurplusFeedIn() {
		// Is Surplus Feed-In activated?
		if (!this.config.activateSurplusFeedIn()) {
			return null;
		}
		// Is battery and inverter data available?
		BatteryData battery = this.core.getBatteryData();
		InverterData inverter = this.core.getInverterData();
		if (battery == null || inverter == null) {
			return null;
		}
		// Is battery full?
		if (battery.getSOE() < 99) {
			return null;
		}
		// Is PV producing?
		int pvPower = Math.round(inverter.getPvPower());
		if (pvPower < 10) {
			return null;
		}
		// Active Surplus feed-in
		return pvPower;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString() //
				+ (this.config.readOnly() ? "|Read-Only-Mode" : "");
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	private Instant lastApplyPower = Instant.MIN;
	private int lastSetActivePower = Integer.MIN_VALUE;

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (this.config.readOnly()) {
			return;
		}

		Settings settings = this.core.getSettings();
		if (settings == null) {
			return;
		}

		Instant now = Instant.now();
		if (this.lastSetActivePower == activePower
				&& Duration.between(this.lastApplyPower, now).getSeconds() < WATCHDOG_SECONDS) {
			// no need to apply to new set-point
			return;
		}

		if (activePower == 0 && this.config.selfRegulationDeactivated()) {
			// avoid setting active power to zero, because this activates 'compensator
			// normal operation'
			settings.setPacSetPoint(0.0001f);

		} else {
			// apply power
			this.logInfo(this.log, "Apply new Active Power [" + activePower + " W].");
			settings.setPacSetPoint(activePower);
		}

		this.lastApplyPower = Instant.now();
		this.lastSetActivePower = activePower;
	}

	@Override
	public int getPowerPrecision() {
		return GlobalUtils.POWER_PRECISION;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		// Read-Only-Mode
		if (this.config.readOnly()) {
			return new Constraint[] {
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		}

		// Surplus Feed-In?
		IntegerReadChannel surplusFeedInChannel = this.channel(BpEss.ChannelId.SURPLUS_FEED_IN);
		Optional<Integer> surplusFeedIn = surplusFeedInChannel.getNextValue().asOptional();
		if (surplusFeedIn.isPresent()) {
			return new Constraint[] { //
					this.createPowerConstraint("Enforce Surplus Feed-In", Phase.ALL, Pwr.ACTIVE,
							Relationship.GREATER_OR_EQUALS, surplusFeedIn.get()) //
			};
		}

		return Power.NO_CONSTRAINTS;
	}

	LocalDateTime lastPowerValuesTimestamp = null;
	double lastPowerValue = 0;
	double accumulatedChargeEnergy = 0;
	double accumulatedDischargeEnergy = 0;

	private void calculateEnergy() {
		if (this.lastPowerValuesTimestamp != null) {

			long passedTimeInMilliSeconds = Duration.between(this.lastPowerValuesTimestamp, LocalDateTime.now())
					.toMillis();
			this.lastPowerValuesTimestamp = LocalDateTime.now();

			double energy = this.lastPowerValue * (passedTimeInMilliSeconds / 1000) / 3600;
			// calculate energy in watt hours

			if (this.lastPowerValue < 0) {
				this.accumulatedChargeEnergy = this.accumulatedChargeEnergy + energy;
				this.getActiveChargeEnergy().setNextValue(accumulatedChargeEnergy);
			} else if (this.lastPowerValue > 0) {
				this.accumulatedDischargeEnergy = this.accumulatedDischargeEnergy + energy;
				this.getActiveDischargeEnergy().setNextValue(accumulatedDischargeEnergy);
			}

		} else {
			this.lastPowerValuesTimestamp = LocalDateTime.now();
		}

		this.lastPowerValue = this.getActivePower().value().orElse(0);
	}

}
