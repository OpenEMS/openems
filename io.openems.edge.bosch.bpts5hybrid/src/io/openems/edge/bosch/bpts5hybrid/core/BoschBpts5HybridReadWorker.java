package io.openems.edge.bosch.bpts5hybrid.core;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;

public class BoschBpts5HybridReadWorker extends AbstractCycleWorker {

	private final Logger log = LoggerFactory.getLogger(BoschBpts5HybridReadWorker.class);
	private final BoschBpts5HybridCoreImpl parent;
	private final BoschBpts5HybridApiClient apiClient;
	private final int refreshIntervalSeconds;
	private LocalDateTime refreshTime;

	public BoschBpts5HybridReadWorker(BoschBpts5HybridCoreImpl parent, String ipaddress, int interval) {
		this.parent = parent;
		this.refreshIntervalSeconds = interval;
		this.apiClient = new BoschBpts5HybridApiClient(ipaddress);
	}

	@Override
	public void activate(String name) {
		super.activate(name);
		this.refreshTime = LocalDateTime.now();
	}

	@Override
	protected void forever() throws Throwable {
		if (this.refreshTime.plusSeconds(this.refreshIntervalSeconds).isBefore(LocalDateTime.now())) {
			this.refreshTime = LocalDateTime.now();

			try {
				this.apiClient.retreiveValues();
				var batterieStatus = this.apiClient.retreiveBatterieStatus();
				if (batterieStatus == 0) {
					this.parent._setSlaveCommunicationFailed(false);
				} else {
					this.parent._setSlaveCommunicationFailed(true);
				}
			} catch (OpenemsException e) {
				this.log.error(e.getMessage());
				this.parent._setSlaveCommunicationFailed(true);
				this.apiClient.connect();
				return;
			}
		}

		this.parent.getEss().ifPresent(ess -> {
			if (this.apiClient.getCurrentDischargePower() > 0) {
				ess._setActivePower(
						this.apiClient.getCurrentDischargePower() + this.apiClient.getCurrentVerbrauchVonPv());
			} else {
				var currentDirectUsageOfPv = this.apiClient.getCurrentVerbrauchVonPv()
						+ this.apiClient.getCurrentEinspeisung();
				ess._setActivePower(currentDirectUsageOfPv);
			}
			ess._setSoc(this.apiClient.getCurrentSoc());
		});

		this.parent.getPv().ifPresent(pv -> {
			pv._setActualPower(this.apiClient.getCurrentPvProduction());
		});

		this.parent.getMeter().ifPresent(meter -> {
			if (this.apiClient.getCurrentStromAusNetz() > 0) {
				meter._setActivePower(this.apiClient.getCurrentStromAusNetz());
			} else if (this.apiClient.getCurrentEinspeisung() > 0) {
				meter._setActivePower(-1 * this.apiClient.getCurrentEinspeisung());
			} else {
				meter._setActivePower(0);
			}
		});
	}

}
