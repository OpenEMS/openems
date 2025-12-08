package io.openems.edge.core.mdns;

import static java.util.stream.Collectors.toMap;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.StreamUtils;
import io.openems.edge.common.mdns.MDnsDiscovery;

@Component
public class MDnsDiscoveryImpl implements MDnsDiscovery {

	private JmDNS jmDns;

	@Activate
	private void activate() throws Exception {
		this.jmDns = JmDNS.create(InetAddress.getLocalHost());
	}

	@Deactivate
	private void deactivate() throws Exception {
		this.jmDns.close();
	}

	@Override
	public AutoCloseable subscribeService(String serviceType, Consumer<MDnsEvent> onChange) {
		final var listener = new MDnsDiscoveryListener(event -> {
			if (event instanceof MDnsEvent.ServiceAdded) {
				this.jmDns.requestServiceInfo(serviceType, event.serviceName(), 0);
			}
			onChange.accept(event);
		});
		this.jmDns.addServiceListener(serviceType, listener);

		return () -> {
			this.jmDns.removeServiceListener(serviceType, listener);
		};
	}

	@Override
	public AutoCloseable subscribeService(String serviceType, String serviceName, Consumer<MDnsEvent> onChange) {
		final var listener = new MDnsDiscoveryListener(event -> {
			if (!event.serviceName().equals(serviceName)) {
				return;
			}
			if (event instanceof MDnsEvent.ServiceAdded) {
				this.jmDns.requestServiceInfo(serviceType, serviceName, 0);
			}
			onChange.accept(event);
		});
		this.jmDns.addServiceListener(serviceType, listener);

		return () -> {
			this.jmDns.removeServiceListener(serviceType, listener);
		};
	}

	private static final class MDnsDiscoveryListener implements ServiceListener {

		private final Logger log = LoggerFactory.getLogger(MDnsDiscoveryListener.class);

		private final Map<String, ServiceEvent> services = new HashMap<>();
		private final Consumer<MDnsEvent> onEvent;

		public MDnsDiscoveryListener(Consumer<MDnsEvent> onEvent) {
			this.onEvent = onEvent;
		}

		@Override
		public void serviceAdded(ServiceEvent serviceEvent) {
			this.log.debug("Service added: {}", serviceEvent.getName());
			this.services.put(serviceEvent.getName(), serviceEvent);
			this.onEvent.accept(new MDnsEvent.ServiceAdded(serviceEvent.getName(), Instant.now()));
		}

		@Override
		public void serviceRemoved(ServiceEvent serviceEvent) {
			this.log.debug("Service removed: {}", serviceEvent.getName());
			this.services.remove(serviceEvent.getName());
			this.onEvent.accept(new MDnsEvent.ServiceRemoved(serviceEvent.getName(), Instant.now()));
		}

		@Override
		public void serviceResolved(ServiceEvent serviceEvent) {
			this.log.debug("Service resolved: {}, {}", serviceEvent.getName(), serviceEvent.getInfo());
			this.services.put(serviceEvent.getName(), serviceEvent);

			final var properties = StreamUtils.enumerationToStream(serviceEvent.getInfo().getPropertyNames()) //
					.collect(toMap(Function.identity(), key -> serviceEvent.getInfo().getPropertyString(key)));

			this.onEvent.accept(new MDnsEvent.ServiceResolved(serviceEvent.getName(), Instant.now(),
					List.of(serviceEvent.getInfo().getInet4Addresses()), properties));
		}
	}

}
