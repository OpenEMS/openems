package io.openems.edge.io.shelly.common;

import io.openems.edge.common.mdns.MDnsDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ShellyMdnsResolver {
	private final Logger log = LoggerFactory.getLogger(ShellyMdnsResolver.class);

	private final String mdnsName;
	private final MDnsDiscovery mDnsDiscovery;
	private final Consumer<String> setIpAddressFunction;

	private AutoCloseable mdnsUnsubscribe;

	public ShellyMdnsResolver(String mdnsName, MDnsDiscovery mDnsDiscovery, Consumer<String> setIpAddressFunction) {
		this.mdnsName = mdnsName;
		this.mDnsDiscovery = mDnsDiscovery;
		this.setIpAddressFunction = setIpAddressFunction;
	}

	/**
	 * Start mdnsDiscovery subscription to search for mdns shelly devices.
	 */
	public void subscribe() {
		this.mdnsUnsubscribe = this.mDnsDiscovery.subscribeService("_shelly._tcp.local.", this.mdnsName, event -> {
			switch (event) {
			case MDnsDiscovery.MDnsEvent.ServiceAdded serviceAdded -> {
				// Do nothing, wait for resolved event
			}
			case MDnsDiscovery.MDnsEvent.ServiceResolved serviceResolved -> {
				if (serviceResolved.addresses().isEmpty()) {
					return;
				}
				final var dynamicIp = serviceResolved.addresses().getFirst();
				this.log.debug("Resolved shelly mdns '{}' to ip address '{}'", this.mdnsName,
						dynamicIp.getHostAddress());
				this.setIpAddressFunction.accept(dynamicIp.getHostAddress());
			}
			case MDnsDiscovery.MDnsEvent.ServiceRemoved serviceRemoved -> {
				this.log.debug("Removed shelly mdns '{}'", this.mdnsName);
				this.setIpAddressFunction.accept(null);
			}
			}
		});
	}

	/**
	 * Unsubscribe and cleanup.
	 */
	public void unsubscribe() {
		if (this.mdnsUnsubscribe != null) {
			try {
				this.mdnsUnsubscribe.close();
			} catch (Exception e) {
				this.log.warn("Error during MDNS unsubscribe for " + this.mdnsName, e);
			}
			this.mdnsUnsubscribe = null;
		}
	}
}
