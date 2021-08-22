package io.openems.edge.controller.evcs.vw.chargesurplus;

import java.time.DayOfWeek;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.io.openems.edge.controller.evcs.vw.chargesurplus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ChargeSurplusControllerImpl extends AbstractOpenemsComponent implements ChargeSurplusController, Controller, OpenemsComponent {

	private static final int ACTUAL_ENERGY_THRESHOLD = -2400;

	private static final int MIN_SOC_ESS = 90;

	private final Logger log = LoggerFactory.getLogger(ChargeSurplusControllerImpl.class);
	
	private static final int MAX_SOC_CHARGING = 80;

	private Config config = null;
	
	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SocEvcs evcs;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricEss ess;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private EssDcCharger pv;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	public ChargeSurplusControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChargeSurplusController.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "evcs", config.evcs_id())) {
			return;
		}
		
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "pv", config.essdccharger_id())) {
			return;
		}
		
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if(ess == null) {
			log.debug("ess not yet set");
			return;
		}
		
		if(meter == null) {
			log.debug("meter not yet set");
			return;
		}
		
		if(evcs == null) {
			log.debug("evcs not yet set");
			return;
		}
		
		Status currentStatus = this.evcs.getStatus();
		int evcsSoc = evcs.getSoc().orElse(Integer.valueOf(0)).intValue();
		int essBatterySoc = this.ess.getSoc().orElse(Integer.valueOf(0)).intValue();
		int meterActivePower = this.meter.getActivePower().orElse(Integer.valueOf(0)).intValue();
		LocalDateTime currentTime = LocalDateTime.now();
		boolean isNightTime = isNightTime(currentTime);
		
		log.debug("Status:"+currentStatus+";EVCS_SoC:"+evcsSoc+";ESS_SoC:"+essBatterySoc+";Meter_Power:"+meterActivePower);
		
		if(currentStatus == Status.STARTING) {
			log.debug("EVCS status state is changing (STARTING) --> doing nothing");
		} else if(currentStatus == Status.CHARGING) {
			if(isNightTime) {
				handleNightTimeChargingStatus(evcsSoc,currentTime);
			}else {
				handleDaytimeChargingStatus(evcsSoc, essBatterySoc);
			}
		} else if(currentStatus == Status.READY_FOR_CHARGING) {
			if(isNightTime) {
				handleNightTimeReadyForCharging(evcsSoc,currentTime);
			}else {
				handleDaytimeReadyForCharging(evcsSoc, essBatterySoc, meterActivePower);
			}
		} else {
			log.debug("Not handled status. Changing nothing.");
		}
	}

	private void handleNightTimeChargingStatus(int evcsSoc, LocalDateTime currentTime) {
		int targetSoc= getTargetSocForWeekday(currentTime);
		
		if(evcsSoc >= targetSoc) {
			log.debug("current EVCS SoC reached target SoC --> stop charging");
			this.evcs._setChargePower(0);
		}
		else {
			log.debug("current EVCS SoC not yet reached the target SoC for weekday. Changing nothing.");
		}		
	}

	private void handleNightTimeReadyForCharging(int evcsSoc, LocalDateTime currentTime) {
		int targetSoc= getTargetSocForWeekday(currentTime);
		
		if(evcsSoc < targetSoc) {
			log.debug("current EVCS SoC below configured Soc"+ evcsSoc+"<"+targetSoc+" --> start charging");
			int chargePower = 480*5;
			this.evcs._setChargePower(chargePower);
		}
		else {
			log.debug("current EVCS SoC equal or above configured Soc. Changing nothing.");
		}
	}
	
	private int getTargetSocForWeekday(LocalDateTime currentTime) {
		int hour = currentTime.getHour();
		
		DayOfWeek dayOfWeek;
		if(hour <= 23 && hour >= 20) {
			dayOfWeek = currentTime.getDayOfWeek().plus(1);
		} else {
			dayOfWeek = currentTime.getDayOfWeek();
		}
		
		int targetSoc = 0;
		
		switch(dayOfWeek) {
		case MONDAY:
			targetSoc = this.config.min_soc_monday();
			break;
		case TUESDAY:
			targetSoc = this.config.min_soc_tuesday();
			break;
		case WEDNESDAY:
			targetSoc = this.config.min_soc_wedtnesday();
			break;
		case THURSDAY:
			targetSoc = this.config.min_soc_thursday();
			break;
		case FRIDAY:
			targetSoc = this.config.min_soc_friday();
			break;
		case SATURDAY:
			targetSoc = this.config.min_soc_saturday();
			break;
		case SUNDAY:
			targetSoc = this.config.min_soc_sunday();
			break;
		}
		
		return targetSoc;
	}

	private boolean isNightTime(LocalDateTime currentTime) {
		int hour = currentTime.getHour();
		return hour >= 22 || hour <= 5;
	}

	private void handleDaytimeReadyForCharging(int evcsSoc, int essBatterySoc, int meterActivePower) {
		if(evcsSoc >= MAX_SOC_CHARGING) {
			log.debug("EVCS SoC is already >="+MAX_SOC_CHARGING+" --> doing nothing");
		}
		else {
			if(essBatterySoc < MIN_SOC_ESS + 2) {
				log.debug("ESS SoC is <"+(MIN_SOC_ESS+2)+" --> doing nothing");
			}
			else {
				if(meterActivePower > ACTUAL_ENERGY_THRESHOLD) {
					log.debug("Actual Energy ("+meterActivePower+") above threshold of "+ACTUAL_ENERGY_THRESHOLD+" --> doing nothing");
				}
				else {
					log.debug("All parameter valid for start charging");
					int chargePower = 480*5;
					this.evcs._setChargePower(chargePower);
				}
			}
		}
	}

	private void handleDaytimeChargingStatus(int evcsSoc, int essBatterySoc) {
		if(evcsSoc >= MAX_SOC_CHARGING) {
			log.debug("EVCS SoC >= "+MAX_SOC_CHARGING+"% ("+evcsSoc+") -> stopping charging process");
			this.evcs._setChargePower(0);
		}
		else {
			if(essBatterySoc < MIN_SOC_ESS) {
				log.debug("EVCS SoC >= "+MAX_SOC_CHARGING+"% ("+evcsSoc+") -> stopping charging process");
				this.evcs._setChargePower(0);
			}
			else {
				log.debug("EVCS is charging & EVCS >="+MAX_SOC_CHARGING+"% & ESS SoC > "+MIN_SOC_ESS+"% --> Change nothing");
			}
		}
	}
}
