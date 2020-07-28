package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import java.io.IOException;
import java.util.List;

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

import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.kaco.blueplanet.hybrid10.BpConstants;
import io.openems.edge.kaco.blueplanet.hybrid10.ErrorChannelId;
import io.openems.edge.kaco.blueplanet.hybrid10.core.BpCore;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Kaco.BlueplanetHybrid10.PvInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		})
public class BpPvInverterImpl extends AbstractOpenemsComponent implements BpPvInverter, ManagedSymmetricPvInverter,
		SymmetricMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(BpPvInverterImpl.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BpCore core;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	public BpPvInverterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ErrorChannelId.values(), //
				BpPvInverter.ChannelId.values() //
		);
		this._setMaxApparentPower(BpConstants.MAX_APPARENT_POWER);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		try {
			// reset limit
			this.setActivePowerLimit(null);
			this.setPvLimitHandler.run();
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(BpPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(BpPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		}
	}

	private void updateChannels() {
		Integer activePower = null;
		Integer reactivePower = null;
		Integer activePowerLimit = null;

		if (!this.core.isConnected()) {
			this.logWarn(this.log, "Core is not connected!");

		} else {
			Settings settings = this.core.getSettings();
			if (settings != null) {
				float eplimitPerc = settings.getEPLimit() / 100;
				activePowerLimit = (int) (BpConstants.MAX_APPARENT_POWER * eplimitPerc);
			}

			Status status = this.core.getStatusData();
			if (status != null) {
				// Set error channels
				List<String> errors = status.getErrors().getErrorCodes();
				for (ErrorChannelId channelId : ErrorChannelId.values()) {
					this.channel(channelId).setNextValue(errors.contains(channelId.getErrorCode()));
				}
			}

			InverterData inverter = this.core.getInverterData();
			if (inverter != null) {
				activePower = (int) inverter.getPvPower();
				reactivePower = 0;
			}
		}

		this._setActivePower(activePower);
		this._setReactivePower(reactivePower);
		this._setActivePowerLimit(activePowerLimit);

		// Calculate Energy
		this.calculateEnergy.update(activePower);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
