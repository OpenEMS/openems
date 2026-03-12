package io.openems.edge.app.meter.shelly.meter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.app.meter.shelly.discovery.ShellyDiscovery;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

@Component
public class ShellyDiscoveryMeter extends ShellyDiscovery<MdnsValueMeter> implements ComponentJsonApi {
	public static final String ID = "shellyDiscoveryMeter";

	private final AppManagerUtil appManagerUtil;

	@Activate
	public ShellyDiscoveryMeter(//
			@Reference MDnsDiscovery mDnsDiscovery, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(mDnsDiscovery);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	public String id() {
		return ShellyDiscoveryMeter.ID;
	}

	@Override
	protected JsonSerializer<MdnsValueMeter> getMdnsValueSerializer() {
		return MdnsValueMeter.serializer();
	}

	@Override
	protected Optional<MdnsValueMeter> createMdnsValue(MDnsDiscovery.MDnsEvent.ServiceResolved resolved) {
		final var type = getShellyType(resolved);
		if (type == null) {
			return Optional.empty();
		}

		return Optional.of(new MdnsValueMeter(resolved.serviceName(), type));
	}

	@Override
	protected List<OpenemsAppInstance> getInstalledAppsByDevice(UUID excludedInstance,
			MDnsDiscovery.MDnsEvent.ServiceResolved resolved) {
		final var shellyApps = this.appManagerUtil.getInstantiatedAppsOfApp("App.Meter.Shelly.Meter");
		return shellyApps.stream().filter(t -> {
			if (t.instanceId.equals(excludedInstance)) {
				return false;
			}

			final var device = t.properties.get(AppShellyMeter.Property.DEVICE.name());
			if (device == null) {
				return false;
			}

			final var mdnsValue = MdnsValueMeter.serializer().deserialize(device);
			return mdnsValue.name().equals(resolved.serviceName());
		}).toList();
	}

	private static ShellyTypeMeter getShellyType(MDnsDiscovery.MDnsEvent.ServiceResolved event) {
		return switch (event.properties().get("app")) {
		case "Pro3EM" -> ShellyTypeMeter.PRO_3EM;
		case null, default -> null;
		};
	}
}
