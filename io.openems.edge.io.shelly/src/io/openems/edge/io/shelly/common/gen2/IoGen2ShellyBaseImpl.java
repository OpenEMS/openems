package io.openems.edge.io.shelly.common.gen2;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.metric.HttpBridgeMetricService;
import io.openems.common.bridge.http.metric.HttpBridgeMetricServiceDefinition;
import io.openems.common.types.DebugMode;
import io.openems.common.types.Result;
import io.openems.common.utils.StringUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.ShellyMdnsResolver;

public abstract class IoGen2ShellyBaseImpl extends AbstractOpenemsComponent
		implements IoGen2ShellyBase, OpenemsComponent {
	private final Logger log = LoggerFactory.getLogger(IoGen2ShellyBaseImpl.class);
	protected String baseUrl;
	protected boolean enableDeviceValidation;

	protected HttpBridgeShellyService shellyService;
	protected ShellyMdnsResolver mdnsResolver;
	protected BridgeHttp httpBridge;
	protected HttpBridgeCycleService cycleService;
	protected HttpBridgeMetricService<String> metricService;

	protected IoGen2ShellyBaseImpl(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) {
		super(//
				firstInitialChannelIds, //
				furtherInitialChannelIds //
		);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String ip,
			String mdnsName, DebugMode debugMode, boolean enableDeviceValidation) {
		super.activate(context, id, alias, enabled);

		this.httpBridge = this.getHttpBridgeFactory().get();
		this.httpBridge.setDebugMode(debugMode);
		if (debugMode == DebugMode.DETAILED) {
			this.metricService = this.httpBridge.createService(HttpBridgeMetricServiceDefinition.byUrl());
		}

		this.cycleService = this.httpBridge.createService(this.getHttpBridgeCycleServiceDefinition());

		if (!enabled) {
			return;
		}

		this.enableDeviceValidation = enableDeviceValidation && this.getSupportedShellyDeviceTypes() != null;
		this.shellyService = this.getHttpBridgeShellyServiceDefinition().create(this.httpBridge, null, null);

		if (!StringUtils.isNullOrBlank(ip)) {
			this.subscribe(ip);
		} else if (!StringUtils.isNullOrBlank(mdnsName)) {
			this.mdnsResolver = new ShellyMdnsResolver(mdnsName, this.getMDnsDiscovery(), this::onIpAddressResolved);
			this.mdnsResolver.subscribe();
		} else {
			this.logWarn(this.log, "No valid IP or MDNS Name configured.");
			this._setSlaveCommunicationFailed(true);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.unsubscribe();

		if (this.mdnsResolver != null) {
			this.mdnsResolver.unsubscribe();
			this.mdnsResolver = null;
		}

		this.getHttpBridgeFactory().unget(this.httpBridge);
		this.httpBridge = null;

		super.deactivate();
	}

	protected void onIpAddressResolved(String ipAddress) {
		this.unsubscribe();
		if (!StringUtils.isNullOrBlank(ipAddress)) {
			this.subscribe(ipAddress);
		}
	}

	protected abstract void subscribeDataCalls();

	private void subscribe(String ip) {
		this.log.info("Subscribing to Shelly at IP {}", ip);

		this.baseUrl = "http://" + ip;
		this.shellyService.setBaseUrl(this.baseUrl);

		if (this.enableDeviceValidation) {
			this.shellyService.subscribeToGen2DeviceInfo(this::onDeviceTypeValidationResult);
		} else {
			this._setWrongDeviceType(false);
		}

		this.subscribeDataCalls();
	}

	private void unsubscribe() {
		this.baseUrl = null;
		try {
			this.shellyService.close();
		} catch (Exception ex) {
			this.logWarn(this.log, "Can't close shelly service: " + ex.getMessage());
		}
		this.cycleService.removeAllCycleEndpoints();
	}

	private void onDeviceTypeValidationResult(Result<Gen2RpcDeviceInfo> result) {
		switch (result) {
		case Result.Ok(var deviceInfo) -> {
			this.setShellyModelName(deviceInfo.app());
			this._setWrongDeviceType(!deviceInfo.isDeviceType(this.getSupportedShellyDeviceTypes()));
		}
		case Result.Error(var exception) -> {
			this.logWarn(this.log, "Can't read shelly device type: " + exception.getMessage());
		}
		}
	}

	protected abstract BridgeHttpFactory getHttpBridgeFactory();

	protected abstract HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition();

	protected abstract HttpBridgeShellyService.HttpBridgeShellyServiceDefinition getHttpBridgeShellyServiceDefinition();

	protected abstract MDnsDiscovery getMDnsDiscovery();

}
