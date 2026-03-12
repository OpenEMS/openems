package io.openems.edge.app.meter.shelly.discovery;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.net.Inet4Address;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.app.meter.shelly.discovery.jsonrpc.GetDiscoveredDevices;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.session.Language;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;

public abstract class ShellyDiscovery<T> implements ComponentJsonApi {
	private static final String SERVICE_TYPE = "_shelly._tcp.local.";

	private final Logger log = LoggerFactory.getLogger(ShellyDiscovery.class);

	private final MDnsDiscovery mDnsDiscovery;

	protected final Map<String, MDnsDiscovery.MDnsEvent.ServiceResolved> discoveredServices = new ConcurrentHashMap<>();
	private CompletableFuture<Void> unsubscribeFuture = CompletableFuture.completedFuture(null);
	private volatile AutoCloseable subscription;

	public ShellyDiscovery(MDnsDiscovery mDnsDiscovery) {
		this.mDnsDiscovery = mDnsDiscovery;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetDiscoveredDevices<>(this.getMdnsValueSerializer()), call -> {
			this.updateSubscribe();

			final var user = call.get(EdgeKeys.USER_KEY);
			final var forInstance = call.getRequest().forInstance();

			final var options = this.discoveredServices.values().stream() //
					.map(resolved -> {
						final var detail = this.buildDetail(resolved);

						var value = this.createMdnsValue(resolved);
						return value.map(v -> new GetDiscoveredDevices.Option<>(//
								resolved.serviceName(), //
								v, //
								detail, //
								this.getDisabledState(forInstance, resolved, user.getLanguage()) //
						)).orElse(null);
					}) //
					.filter(Objects::nonNull) //
					.toList();
			return new GetDiscoveredDevices.Response<>(options);
		});
	}

	protected String buildDetail(MDnsDiscovery.MDnsEvent.ServiceResolved resolved) {
		return "IP: " + resolved.addresses().stream() //
				.map(Inet4Address::getHostAddress) //
				.reduce((a, b) -> a + ", " + b).orElse("N/A") //
				+ resolved.properties().entrySet().stream() //
				.map(e -> e.getKey() + ": " + e.getValue()) //
				.collect(Collectors.joining(", ", ", ", ""));
	}

	protected abstract JsonSerializer<T> getMdnsValueSerializer();

	protected abstract Optional<T> createMdnsValue(MDnsDiscovery.MDnsEvent.ServiceResolved resolved);

	protected abstract List<OpenemsAppInstance> getInstalledAppsByDevice(UUID excludedInstance, MDnsDiscovery.MDnsEvent.ServiceResolved resolved);

	protected Optional<String> computeDeviceAlreadyInUseText(UUID excludedInstance, MDnsDiscovery.MDnsEvent.ServiceResolved resolved, Language l) {
		var installedAppsByDevice = this.getInstalledAppsByDevice(excludedInstance, resolved);
		if (installedAppsByDevice.isEmpty()) {
			return Optional.empty();
		}

		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		final var deviceInUseText = translate(bundle, "App.Meter.Shelly.device.alreadyUsed", installedAppsByDevice.stream() //
				.map(t -> t.alias) //
				.collect(Collectors.joining(", ")));

		return Optional.of(deviceInUseText);
	}

	private GetDiscoveredDevices.OptionState getDisabledState(UUID forInstance, MDnsDiscovery.MDnsEvent.ServiceResolved resolved, Language l) {
		var deviceInUseText = this.computeDeviceAlreadyInUseText(forInstance, resolved, l);
		return deviceInUseText.map(text -> new GetDiscoveredDevices.OptionState(true, text)).orElse(null);
	}

	protected synchronized void updateSubscribe() {
		if (this.subscription != null) {
			return;
		}

		this.subscription = this.mDnsDiscovery.subscribeService(SERVICE_TYPE, event -> {
			switch (event) {
			case MDnsDiscovery.MDnsEvent.ServiceAdded added -> {
			}
			case MDnsDiscovery.MDnsEvent.ServiceResolved resolved -> {
				this.discoveredServices.put(resolved.serviceName(), resolved);
			}
			case MDnsDiscovery.MDnsEvent.ServiceRemoved removed -> {
				this.discoveredServices.remove(removed.serviceName());
			}
			}
		});

		this.unsubscribeFuture.cancel(false);
		this.unsubscribeFuture = CompletableFuture.runAsync(() -> {
			try {
				this.discoveredServices.clear();
				this.subscription.close();
				this.subscription = null;
			} catch (Exception e) {
				this.log.warn("Error during MDNS unsubscribe: {}", e.getMessage());
			}
		}, CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES));
	}

}
