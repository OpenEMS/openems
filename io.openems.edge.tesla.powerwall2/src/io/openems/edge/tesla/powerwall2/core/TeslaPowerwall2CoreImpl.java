package io.openems.edge.tesla.powerwall2.core;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.tesla.powerwall2.battery.TeslaPowerwall2Battery;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tesla.Powerwall2.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class TeslaPowerwall2CoreImpl extends AbstractOpenemsComponent
		implements TeslaPowerwall2Core, OpenemsComponent, EventHandler {

	private ReadWorker worker = null;

	private final AtomicReference<TeslaPowerwall2Battery> battery = new AtomicReference<>();

	public TeslaPowerwall2CoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TeslaPowerwall2Core.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config)
			throws UnknownHostException, KeyManagementException, NoSuchAlgorithmException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.worker = new ReadWorker(this, (Inet4Address) InetAddress.getByName(config.ipAddress()), config.port());
		this.worker.activate(config.id());
	}

	@Override
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
		}
	}

	@Override
	public void setBattery(TeslaPowerwall2Battery battery) {
		this.battery.set(battery);
	}

	@Override
	public Optional<TeslaPowerwall2Battery> getBattery() {
		return Optional.ofNullable(this.battery.get());
	}

}
