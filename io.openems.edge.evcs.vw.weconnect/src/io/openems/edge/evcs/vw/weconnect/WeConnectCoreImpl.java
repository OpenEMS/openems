package io.openems.edge.evcs.vw.weconnect;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.api.Status;

import java.io.IOException;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.evcs.vw.weconnect", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class WeConnectCoreImpl extends AbstractOpenemsComponent implements WeConnectCore, OpenemsComponent, EventHandler, SocEvcs, Evcs {

	private final Logger log = LoggerFactory.getLogger(WeConnectCoreImpl.class);
	
	WeConnectReadWorker worker = null;
	private int chargeRequestCount = 0;
	private int chargeRejectCount = 0;
	
	public enum ChargeRequestState {
		START_CHARGING,
		STOP_CHARGING,
		UNDEFINED
	}
	
    @Reference
	private ConfigurationAdmin cm;

	public WeConnectCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				SocEvcs.ChannelId.values(), //
				WeConnectCore.ChannelId.values() //
		);
		
		this._setStatus(Status.STARTING);
		this._setChargingType(ChargingType.CCS);
		this._setPhases(3);
		this._setActiveConsumptionEnergy(0);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ConfigurationException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		
		chargeRequestCount = 0;
		chargeRejectCount = 0;
		
		this.worker = new WeConnectReadWorker(this, config);
		this.worker.activate(config.id());
	}
	
	@Deactivate
	protected void deactivate() {
		if (this.worker != null) {
			this.worker.deactivate();
		}
		
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.worker == null) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.worker.triggerNextRun();
			break;
		default:
			break;
		}
	}

	@Override
	public String debugLog() {
		return "currentSoc:"+this.getSoc().get()+";currentStatus:"+this.getStatus()+";chargePower:"+this.getChargePower()+";activeConsumptionEnergy:"+this.getActiveConsumptionEnergy();
	}
	
	@Override
	public void _setChargePower(Integer value) {
		this.getChargePowerChannel().setNextValue(value);
		
		if(value > 0) {
			chargeRequestCount ++;
		}
		else {
			chargeRejectCount ++;
		}
	}
	
	@Override
	public void _setChargePower(int value) {
		this._setChargePower(Integer.valueOf(value));
	}

	public ChargeRequestState getChargingRequested() {
		if(chargeRejectCount==0 && chargeRequestCount==0) {
			log.error("isChargingRequested: result=UNDEFINED chargeRequestCount:"+chargeRequestCount+" chargeRejectCount:"+chargeRejectCount);
			return ChargeRequestState.UNDEFINED;
		}
		
		if(chargeRequestCount >= chargeRejectCount) {
			chargeRejectCount = 0;
			chargeRequestCount = 0;
			log.error("isChargingRequested: result=START_CHARGING chargeRequestCount:"+chargeRequestCount+" chargeRejectCount:"+chargeRejectCount);
			return ChargeRequestState.START_CHARGING;
		}
		else{
			chargeRejectCount = 0;
			chargeRequestCount = 0;
			log.error("isChargingRequested: result=STOP_CHARGING chargeRequestCount:"+chargeRequestCount+" chargeRejectCount:"+chargeRejectCount);
			return ChargeRequestState.STOP_CHARGING;
		}
	}
}

