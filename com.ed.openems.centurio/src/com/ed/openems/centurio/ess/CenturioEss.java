package com.ed.openems.centurio.ess;

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

import com.ed.data.BatteryData;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.openems.centurio.CenturioConstants;
import com.ed.openems.centurio.datasource.api.EdComData;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyDepot.CenturioEss", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class CenturioEss extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	protected final static int MAX_APPARENT_POWER = 10000;

	private final Logger log = LoggerFactory.getLogger(CenturioEss.class);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected EdComData datasource;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	private boolean readonly = false;

	public CenturioEss() {
		EssUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Datasource", config.datasource_id())) {
			return;
		}

		this.readonly = config.readonly();
		if (readonly) {
			// Do not allow Power in read-only mode
			this.getMaxApparentPower().setNextValue(0);
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.updateChannels();
			break;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		A001(new Doc().level(Level.WARNING).text("Emergency Stop")), //
		A002(new Doc().level(Level.WARNING).text("Key Manual Stop")), //
		A003(new Doc().level(Level.WARNING).text("Transformer Phase B Temperature Sensor Invalidation")), //
		A004(new Doc().level(Level.WARNING).text("SD Memory Card Invalidation")), //
		A005(new Doc().level(Level.WARNING).text("Inverter Communication Abnormity")), //
		A010(new Doc().level(Level.WARNING).text("Battery Stack Communication Abnormity")), //
		A021(new Doc().level(Level.WARNING).text("Multifunctional Ammeter Communication Abnormity")), //
		A022(new Doc().level(Level.WARNING).text("Remote Communication Abnormity")), //
		A030(new Doc().level(Level.WARNING).text("PVDC1 Communication Abnormity")), //
		A032(new Doc().level(Level.WARNING).text("PVDC2 Communication Abnormity")), //
		A040(new Doc().level(Level.WARNING).text("Transformer Severe Overtemperature")), //
		A050(new Doc().level(Level.WARNING).text("DC Precharge Contactor Close Unsuccessfully")), //
		A060(new Doc().level(Level.WARNING).text("AC Precharge Contactor Close Unsuccessfully")), //
		A071(new Doc().level(Level.WARNING).text("AC Main Contactor Close Unsuccessfully")), //
		A072(new Doc().level(Level.WARNING).text("DC Electrical Breaker1 Close Unsuccessfully")), //
		A100(new Doc().level(Level.WARNING).text("DC Main Contactor Close Unsuccessfully")), //
		A110(new Doc().level(Level.WARNING).text("AC Breaker Trip")), //
		A200(new Doc().level(Level.WARNING).text("AC Main Contactor Open When Running")), //
		A210(new Doc().level(Level.WARNING).text("DC Main Contactor Open When Running")), //
		A220(new Doc().level(Level.WARNING).text("AC Main Contactor Open Unsuccessfully")), //
		A230(new Doc().level(Level.WARNING).text("DC Electrical Breaker1 Open Unsuccessfully")), //

		E001(new Doc().level(Level.FAULT).text("DC Main Contactor Open Unsuccessfully")), //
		E002(new Doc().level(Level.FAULT).text("Hardware PDP Fault")), //
		E010(new Doc().level(Level.FAULT).text("Master Stop Suddenly")), //
		E021(new Doc().level(Level.FAULT).text("DCShortCircuitProtection")), //
		E022(new Doc().level(Level.FAULT).text("DCOvervoltageProtection")), //
		E030(new Doc().level(Level.FAULT).text("DCUndervoltageProtection")), //
		E041(new Doc().level(Level.FAULT).text("DCInverseNoConnectionProtection")), //
		E042(new Doc().level(Level.FAULT).text("DCDisconnectionProtection")), //
		E050(new Doc().level(Level.FAULT).text("CommutingVoltageAbnormityProtection")), //
		E060(new Doc().level(Level.FAULT).text("DCOvercurrentProtection")), //
		E070(new Doc().level(Level.FAULT).text("Phase1PeakCurrentOverLimitProtection")), //
		E080(new Doc().level(Level.FAULT).text("Phase2PeakCurrentOverLimitProtection")), //
		E101(new Doc().level(Level.FAULT).text("Phase3PeakCurrentOverLimitProtection")), //
		E102(new Doc().level(Level.FAULT).text("Phase1GridVoltageSamplingInvalidation")), //
		E103(new Doc().level(Level.FAULT).text("Phase2VirtualCurrentOverLimitProtection")), //
		E104(new Doc().level(Level.FAULT).text("Phase3VirtualCurrentOverLimitProtection")), //
		E110(new Doc().level(Level.FAULT).text("Phase1GridVoltageSamplingInvalidation2")), //
		E120(new Doc().level(Level.FAULT).text("Phase2ridVoltageSamplingInvalidation")), //
		E140(new Doc().level(Level.FAULT).text("Phase3GridVoltageSamplingInvalidation")), //
		E150(new Doc().level(Level.FAULT).text("Phase1InvertVoltageSamplingInvalidation")), //
		E160(new Doc().level(Level.FAULT).text("Phase2InvertVoltageSamplingInvalidation")), //
		E170(new Doc().level(Level.FAULT).text("Phase3InvertVoltageSamplingInvalidation")), //
		E180(new Doc().level(Level.FAULT).text("ACCurrentSamplingInvalidation")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	private void updateChannels() {
		Integer soc = null;
		Integer activePower = null;
		Integer reactivePower = null;
		GridMode gridMode = GridMode.UNDEFINED;

		if (!this.datasource.isConnected()) {
			this.logWarn(this.log, "Edcom is not connected!");

		} else {
			BatteryData battery = this.datasource.getBatteryData();
			Status status = this.datasource.getStatusData();
			InverterData inverter = this.datasource.getInverterData();

			if (battery != null) {
				soc = Math.round(battery.getSOE());
				activePower = CenturioConstants.roundToPowerPrecision(battery.getPower()) * -1; // invert
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
				default:
					gridMode = GridMode.UNDEFINED;
				}

				// Set error channels
				List<String> errors = status.getErrors().getErrorCodes();
				for (Channel<?> channel : this.channels()) {
					if (channel instanceof CenturioErrorChannel) {
						CenturioErrorChannel c = (CenturioErrorChannel) channel;
						c.setNextValue(errors.contains(c.getErrorCode()));
					}
				}
			}

			if (inverter != null) {
				reactivePower = (CenturioConstants.roundToPowerPrecision(inverter.getReactivPower(0))
						+ CenturioConstants.roundToPowerPrecision(inverter.getReactivPower(1))
						+ CenturioConstants.roundToPowerPrecision(inverter.getReactivPower(2))) * -1;
			}
		}

		this.getSoc().setNextValue(soc);
		this.getActivePower().setNextValue(activePower);
		this.getReactivePower().setNextValue(reactivePower);
		this.getGridMode().setNextValue(gridMode);

		// Set ALLOWED_CHARGE_POWER and ALLOWED_DISCHARGE_POWER
		if (soc == null || soc > 99) {
			this.getAllowedCharge().setNextValue(0);
		} else {
			this.getAllowedCharge().setNextValue(MAX_APPARENT_POWER * -1);
		}
		if (soc == null || soc < 0) {
			this.getAllowedDischarge().setNextValue(0);
		} else {
			this.getAllowedDischarge().setNextValue(MAX_APPARENT_POWER);
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		Settings settings = this.datasource.getSettings();
		if (settings == null) {
			return;
		}

		// avoid setting active power to zero, because this activates 'compensator
		// normal operation'
		if (activePower == 0) {
			if (this.getSoc().value().orElse(0) > 50) {
				activePower = 1; // > 50 % SoC: discharge
			} else {
				activePower = -1; // <= 50 % SoC: discharge
			}
		}

		// Log output on changed power
		int lastActivePower = Math.round(settings.getPacSetPoint()) * -1;
		if (lastActivePower != activePower) {
			this.logInfo(this.log,
					"Apply new Active Power [" + activePower + " W]. Last value was [" + lastActivePower + " W]");
		}

		// apply power
		settings.setPacSetPoint(activePower * -1);
	}

	@Override
	public int getPowerPrecision() {
		return CenturioConstants.POWER_PRECISION;
	}

}
