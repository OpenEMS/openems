package io.openems.edge.bosch.bpts5hybrid.core;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bosch.bpts5hybrid.ess.BoschBpts5HybridEss;
import io.openems.edge.bosch.bpts5hybrid.meter.BoschBpts5HybridMeter;
import io.openems.edge.bosch.bpts5hybrid.pv.BoschBpts5HybridPv;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bosch.BPTS5Hybrid.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class BoschBpts5HybridCoreImpl extends AbstractOpenemsComponent
		implements BoschBpts5HybridCore, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(BoschBpts5HybridCoreImpl.class);

	private final AtomicReference<BoschBpts5HybridEss> ess = new AtomicReference<>();
	private final AtomicReference<BoschBpts5HybridPv> pv = new AtomicReference<>();
	private final AtomicReference<BoschBpts5HybridMeter> meter = new AtomicReference<>();
	private final BoschBpts5HybridApiClient apiClient = new BoschBpts5HybridApiClient();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;
	private String baseUrl;
	private int refreshIntervalSeconds;
	private long lastRefreshTime = 0;

	public BoschBpts5HybridCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				CoreChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws ConfigurationException, IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!config.enabled()) {
			return;
		}
		this.baseUrl = "http://" + config.ipaddress();
		this.refreshIntervalSeconds = config.interval();
		this.httpBridge = this.httpBridgeFactory.get();

		// Initial connect to get WUI_SID
		this.httpBridge.get(this.baseUrl).thenAccept(response -> {
			try {
				this.apiClient.processConnectResponse(response.data());
			} catch (Exception e) {
				this.log.warn("Initial connect to Bosch BPT-S 5 failed: " + e.getMessage());
			}
		});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.httpBridge == null) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.pollData();
			break;
		}
	}

	private void pollData() {
		var now = System.currentTimeMillis();
		if (now - this.lastRefreshTime < this.refreshIntervalSeconds * 1000L) {
			return;
		}
		this.lastRefreshTime = now;

		if (!this.apiClient.isConnected()) {
			// Reconnect
			this.httpBridge.get(this.baseUrl).thenAccept(response -> {
				try {
					this.apiClient.processConnectResponse(response.data());
				} catch (Exception e) {
					this.log.warn("Reconnect to Bosch BPT-S 5 failed: " + e.getMessage());
					this._setSlaveCommunicationFailed(true);
				}
			});
			return;
		}

		// Retrieve values via POST
		this.httpBridge.post(this.apiClient.getValuesUrl(this.baseUrl), BoschBpts5HybridApiClient.getPostRequestData())
				.thenAccept(response -> {
					try {
						this.apiClient.processValuesResponse(response.data());
						this.updateChannels();
					} catch (Exception e) {
						this.log.warn("Error reading values from Bosch BPT-S 5: " + e.getMessage());
						this._setSlaveCommunicationFailed(true);
					}
				});

		// Retrieve battery status via GET
		this.httpBridge.get(this.apiClient.getBatteryStatusUrl(this.baseUrl)).thenAccept(response -> {
			try {
				var status = this.apiClient.processBatteryStatusResponse(response.data());
				this._setSlaveCommunicationFailed(status != 0);
			} catch (Exception e) {
				this.log.warn("Error reading battery status from Bosch BPT-S 5: " + e.getMessage());
				this._setSlaveCommunicationFailed(true);
			}
		});
	}

	private void updateChannels() {
		this.getEss().ifPresent(ess -> {
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

		this.getPv().ifPresent(pv -> {
			pv._setActualPower(this.apiClient.getCurrentPvProduction());
		});

		this.getMeter().ifPresent(meter -> {
			if (this.apiClient.getCurrentStromAusNetz() > 0) {
				meter._setActivePower(this.apiClient.getCurrentStromAusNetz());
			} else if (this.apiClient.getCurrentEinspeisung() > 0) {
				meter._setActivePower(-1 * this.apiClient.getCurrentEinspeisung());
			} else {
				meter._setActivePower(0);
			}
		});
	}

	@Override
	public void setEss(BoschBpts5HybridEss boschBpts5HybridEss) {
		this.ess.set(boschBpts5HybridEss);
	}

	@Override
	public Optional<BoschBpts5HybridEss> getEss() {
		return Optional.ofNullable(this.ess.get());
	}

	@Override
	public void setPv(BoschBpts5HybridPv boschBpts5HybridPv) {
		this.pv.set(boschBpts5HybridPv);
	}

	@Override
	public Optional<BoschBpts5HybridPv> getPv() {
		return Optional.ofNullable(this.pv.get());
	}

	@Override
	public void setMeter(BoschBpts5HybridMeter boschBpts5HybridMeter) {
		this.meter.set(boschBpts5HybridMeter);
	}

	@Override
	public Optional<BoschBpts5HybridMeter> getMeter() {
		return Optional.ofNullable(this.meter.get());
	}
}
