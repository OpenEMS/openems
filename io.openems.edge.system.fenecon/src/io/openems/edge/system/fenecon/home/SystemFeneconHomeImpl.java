package io.openems.edge.system.fenecon.home;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;
import io.openems.edge.system.fenecon.home.enums.LedOrder;
import io.openems.edge.system.fenecon.home.enums.StateLed;

@Designate(ocd = io.openems.edge.system.fenecon.home.Config.class, factory = true)
@Component(//
		name = "System.Fenecon.Home", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class SystemFeneconHomeImpl extends AbstractOpenemsComponent
		implements SystemFeneconHome, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(SystemFeneconHomeImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(enabled=true)")
	private volatile ControllerApiBackend backend;

	private Instant timeStampForLedBlinks;
	private boolean isLedOnForBlinks = true;
	private LedOrder.Actual ledOrder;

	public SystemFeneconHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SystemFeneconHome.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) {
		this.ledOrder = config.ledOrder().toActual(config.relayId());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			var stateLed = StateLed.determineFrom(this.sum, this.backend);
			this.updateStateLed(stateLed);
		}
		}
	}

	/**
	 * Sets the Relays to update the State LED.
	 * 
	 * @param stateLed {@link StateLed}; possibly null
	 */
	private void updateStateLed(StateLed stateLed) {
		if (stateLed == null) {
			this.turnOffChannels(this.ledOrder.getAllChannelAddresses());
			return;
		}

		/* Switch ON the target relay */
		final var channelAddressToTurnOn = this.ledOrder.getChannelAddressForColor(stateLed.color);
		this.turnOnChannels(channelAddressToTurnOn, stateLed.isPermanent);

		/* Switch OFF the other relays */
		final var channelAddressesToTurnOff = this.ledOrder.getRemainingChannelAddressesForColor(stateLed.color);
		this.turnOffChannels(channelAddressesToTurnOff);
	}

	private void turnOnChannels(ChannelAddress channelAddress, boolean isPermanent) {
		try {
			final BooleanWriteChannel outputChannel = this.componentManager.getChannel(channelAddress);
			final boolean setWriteValue;

			if (isPermanent) {
				setWriteValue = true;

			} else {
				final var now = Instant.now(this.componentManager.getClock());
				if (this.timeStampForLedBlinks == null) {
					this.timeStampForLedBlinks = now;
				}
				var durationBetweenBlinks = Duration.between(this.timeStampForLedBlinks, now);
				if (durationBetweenBlinks.getSeconds() >= 1) {
					this.timeStampForLedBlinks = now;
					this.isLedOnForBlinks = !this.isLedOnForBlinks;
				}
				setWriteValue = this.isLedOnForBlinks;
			}

			setWriteValueIfNotRead(outputChannel, setWriteValue);

		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to Turn-On-Channels: " + e.getMessage());
		}
	}

	private void turnOffChannels(ChannelAddress[] channelAddresses) {
		for (var channelAddress : channelAddresses) {
			try {
				final BooleanWriteChannel outputChannel = this.componentManager.getChannel(channelAddress);
				setWriteValueIfNotRead(outputChannel, false);

			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to Turn-On-Channels: " + e.getMessage());
			}
		}
	}
}
