package com.ed.openems.centurio.ess;

import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.optim.linear.Relationship;
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

import com.ed.data.BatteryData;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.openems.centurio.datasource.api.EdComData;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;


@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyDepot.CenturioEss", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "="
				+ EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class CenturioEss extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected EdComData datasource;
	@Reference
	protected ConfigurationAdmin cm;
	
	@Reference
	private Power power;
	private List<String> errors;
	private int maxApparentPower = 0;
	private Constraint allowedChargeConstraint;
	private Constraint allowedDischargeConstraint;

	protected final static int MAX_APPARENT_POWER = 40000;
	



	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.maxApparentPower = config.maxP();
		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Datasource", config.datasource_id())) {
			return;
		}
		this.getMaxActivePower().setNextValue(config.maxP());
		
		/*
		 * Initialize Power
		 */
		
		// Allowed Charge
		this.allowedChargeConstraint = this.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE,
				Relationship.GEQ, 0 /* initial zero; is set later */);
		// Allowed Discharge
		this.allowedDischargeConstraint = this.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE,
				Relationship.LEQ, 0 /* initial zero; is set later */);		

	
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public CenturioEss() {
		EssUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		

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
		BatteryData battery = this.datasource.getBatteryData();
		Status status = this.datasource.getStatusData();
		InverterData invdata = this.datasource.getInverterData();

		this.getSoc().setNextValue((int)battery.getSOE());
		this.getActivePower().setNextValue(Math.round(battery.getPower()/10) * -10);

		this.getReactivePower().setNextValue((Math.round(invdata.getReactivPower(0)/10) * -10) + Math.round(invdata.getReactivPower(1)/10) * (-10)
				+ Math.round(invdata.getReactivPower(2)/10) * (-10));

		int invStatus = status.getInverterStatus();

		switch (invStatus) {
		case 12:
			this.getGridMode().setNextValue(2);
		case 13:
		case 14:
			this.getGridMode().setNextValue(1);
		default:
			this.getGridMode().setNextValue(0);
		}

		errors = status.getErrors().getErrorCodes();
		for (String error : errors) {
			
			
			ChannelId ch;
			try {
				ch = CenturioEss.ChannelId.valueOf(error);
				this.channel(ch).setNextValue(true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}

	}

	@Override
	public String debugLog() {
		//return "GridMode: " + this.getGridMode().value().toString();
		 return "Battery Power: " + this.getActivePower().value().toString();

	}


	@Override
	public Power getPower() {
		
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		Settings settings = this.datasource.getSettings();
		
		float soc = this.datasource.getBatteryData().getSOE();
		
		if (soc == 0 && activePower > 0) {
			activePower = 0;
		}
		if (soc == 100 && activePower < 0) {
			activePower = 0;
		}
			
			
		
		if (soc == 100) {
			this.allowedChargeConstraint.setIntValue(0);
			
		} else {
			this.allowedChargeConstraint.setIntValue(this.maxApparentPower * -1);
		}
		if (soc == 0) {
			this.allowedDischargeConstraint.setIntValue(0);
			
		} else {
			this.allowedDischargeConstraint.setIntValue(this.maxApparentPower);
		}
		float old = settings.getPacSetPoint();
		if(old != activePower) {
			System.out.println("Old power set for Centurio: " + old + " W; " + "New: " + activePower + " W");
			settings.setPacSetPoint(activePower);
		}
		
	}

	@Override
	public int getPowerPrecision() {
		
		return 10;
	}

}
