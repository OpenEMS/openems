package io.openems.edge.evcs.vw.weconnect;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.evcs.api.Status;

public class WeConnectReadWorker extends AbstractCycleWorker{

	private final Logger log = LoggerFactory.getLogger(WeConnectReadWorker.class);
	private final WeConnectCoreImpl parent;
	private final WeConnectApiClient apiClient;
	
	public static int refreshIntervalSeconds = 60;
	LocalDateTime refreshTime = null;
	int chargingStatusChangeCooldown = 5;
	
	public WeConnectReadWorker(WeConnectCoreImpl parent,Config config) {
		this.parent = parent;
		this.apiClient = new WeConnectApiClient(config);
	}
	
	@Override
	public void activate(String name) {
		super.activate(name);
		this.apiClient.login();
		this.apiClient.update();
		this.refreshTime = LocalDateTime.now();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		this.apiClient.logout();
	}
	
	@Override
	protected void forever() throws Throwable {
		
		if (this.refreshTime.plusSeconds(refreshIntervalSeconds).isBefore(LocalDateTime.now())) {
			this.refreshTime = LocalDateTime.now();
			this.apiClient.update();
			
			this.parent._setSoc(this.apiClient.getCurrentSoC());
			Status currentStatus = this.apiClient.getCurrentStatus();
			this.parent._setStatus(currentStatus);
			
			switch(this.parent.getChargingRequested()) {
			case START_CHARGING:
			{
				log.error("charging requested");
				
				if(currentStatus != Status.CHARGING && currentStatus == Status.READY_FOR_CHARGING) {
					log.error("sentStartCharging");
					
					this.parent._setStatus(Status.STARTING);
					this.apiClient.sentStartCharging();
				}
				else {
					log.error("charging requested: but already charging or charging not possible");
				}
				break;
			}
			case STOP_CHARGING:
			{
				log.error("charging stop requested");
				
				if(currentStatus == Status.CHARGING) {
					log.error("sentStopCharging");
					
					this.parent._setStatus(Status.STARTING);
					this.apiClient.sentStopCharging();
				}				
				break;
			}
			default:
				log.error("undefined --> doing nothing");
				break;
			}
		}
	}

}
