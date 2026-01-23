package io.openems.edge.app.meter.shelly;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.net.Inet4Address;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.session.Language;
import io.openems.edge.app.meter.shelly.jsonrpc.GetOptions;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppManagerUtil;

@Component
public final class ShellyDiscovery implements ComponentJsonApi {

	private static final String SERVICE_TYPE = "_shelly._tcp.local.";
	public static final String ID = "shellyDiscovery";

	private final Logger log = LoggerFactory.getLogger(ShellyDiscovery.class);

	private final MDnsDiscovery mDnsDiscovery;
	private final AppManagerUtil appManagerUtil;

	private final Map<String, MDnsDiscovery.MDnsEvent.ServiceResolved> discoveredServices = new ConcurrentHashMap<>();
	private CompletableFuture<Void> unsubscribeFuture = CompletableFuture.completedFuture(null);
	private volatile AutoCloseable subscription;

	@Activate
	public ShellyDiscovery(//
			@Reference MDnsDiscovery mDnsDiscovery, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		this.mDnsDiscovery = mDnsDiscovery;
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	public String id() {
		return ShellyDiscovery.ID;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetOptions<>(MdnsValue.serializer()), call -> {
			this.updateSubscribe();

			final var user = call.get(EdgeKeys.USER_KEY);
			final var forInstance = call.getRequest().forInstance();

			final var options = this.discoveredServices.values().stream() //
					.map(resolved -> {
						final var detail = "IP: " + resolved.addresses().stream() //
								.map(Inet4Address::getHostAddress) //
								.reduce((a, b) -> a + ", " + b).orElse("N/A") //
								+ resolved.properties().entrySet().stream() //
										.map(e -> e.getKey() + ": " + e.getValue()) //
										.collect(Collectors.joining(", ", ", ", ""));

						final var type = getShellyType(resolved);
						if (type == null) {
							return null;
						}

						return new GetOptions.Option<>(//
								resolved.serviceName(), //
								new MdnsValue(resolved.serviceName(), type), //
								detail, //
								this.getDisabledState(forInstance, resolved.serviceName(), user.getLanguage()) //
						);
					}) //
					.filter(Objects::nonNull) //
					.toList();
			return new GetOptions.Response<>(options);
		});
	}

	private GetOptions.OptionState getDisabledState(UUID forInstance, String serviceName, Language l) {
		final var shellyApps = this.appManagerUtil.getInstantiatedAppsOfApp("App.Meter.Shelly");
		final var usingApps = shellyApps.stream().filter(t -> {
			if (t.instanceId.equals(forInstance)) {
				return false;
			}
			final var device = t.properties.get(AppShellyMeter.Property.DEVICE.name());
			if (device == null) {
				return false;
			}
			final var mdnsValue = MdnsValue.serializer().deserialize(device);
			return mdnsValue.name().equals(serviceName);
		}).toList();

		if (usingApps.isEmpty()) {
			return null;
		}

		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return new GetOptions.OptionState(//
				true, //
				translate(bundle, "App.Meter.Shelly.device.alreadyUsed", usingApps.stream() //
						.map(t -> t.alias) //
						.collect(Collectors.joining(", "))) //
		);
	}

	private static ShellyType getShellyType(MDnsDiscovery.MDnsEvent.ServiceResolved event) {
		return switch (event.properties().get("app")) {
		case "PlusPlugS" -> ShellyType.PLUS_PLUG_S;
		case "OutdoorPlugSG3" -> ShellyType.OUTDOOR_PLUG_S_GEN_3;
		case "PlugSG3" -> ShellyType.PLUG_S_GEN_3;
		case null, default -> null;
		};
	}

	private synchronized void updateSubscribe() {
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
