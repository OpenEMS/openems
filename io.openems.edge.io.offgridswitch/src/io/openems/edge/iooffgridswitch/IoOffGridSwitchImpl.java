package io.openems.edge.iooffgridswitch;

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

import com.google.common.base.Objects;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Io.Off.Grid.Switch", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class IoOffGridSwitchImpl extends AbstractOpenemsComponent
		implements IoOffGridSwitch, OffGridSwitch, OpenemsComponent, EventHandler {

	@Reference
	private ComponentManager componentManager;

	private ChannelAddress inputMainContactorChannelAddr;
	private ChannelAddress inputGridStatusChannelAddr;
	private ChannelAddress inputGroundingContactorChannelAddr;

	private ChannelAddress outputMainContactorChannelAddr;
	private ChannelAddress outputGroundingContactorChannelAddr;

	public IoOffGridSwitchImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				OffGridSwitch.ChannelId.values(), //
				IoOffGridSwitch.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.inputMainContactorChannelAddr = ChannelAddress.fromString(config.inputMainContactor());
		this.inputGridStatusChannelAddr = ChannelAddress.fromString(config.inputGridStatus());
		this.inputGroundingContactorChannelAddr = ChannelAddress.fromString(config.inputGroundingContactor());
		this.outputMainContactorChannelAddr = ChannelAddress.fromString(config.outputMainContactor());
		this.outputGroundingContactorChannelAddr = ChannelAddress.fromString(config.outputGroundingContactor());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.handleInput();
			break;
		}
	}

	/**
	 * Handle Digital Inputs and convert to {@link OffGridSwitch} Channels.
	 */
	private void handleInput() {
		this._setMainContactor(this.getInputChannel(this.inputMainContactorChannelAddr));

		var inputGridStatus = this.getInputChannel(this.inputGridStatusChannelAddr);
		if (inputGridStatus == null) {
			this._setGridMode(GridMode.UNDEFINED);
		} else if (inputGridStatus) {
			this._setGridMode(GridMode.OFF_GRID);
		} else {
			this._setGridMode(GridMode.ON_GRID);
		}

		this._setGroundingContactor(this.getInputChannel(this.inputGroundingContactorChannelAddr));
	}

	/**
	 * Read a Digital Input.
	 *
	 * @param channelAddress the {@link ChannelAddress}
	 * @return true, false or null
	 */
	private Boolean getInputChannel(ChannelAddress channelAddress) {
		try {
			BooleanReadChannel channel = this.componentManager.getChannel(channelAddress);
			return channel.value().get();
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			return null;
		}
	}

	@Override
	public void setMainContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(this.outputMainContactorChannelAddr, operation, Relay.NORMALLY_CLOSED);
	}

	@Override
	public void setGroundingContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(this.outputGroundingContactorChannelAddr, operation, Relay.NORMALLY_OPEN);
	}

	private static enum Relay {
		NORMALLY_OPEN, NORMALLY_CLOSED;
	}

	/**
	 * Sets the Output.
	 *
	 * @param channelAddress Address of the {@link BooleanWriteChannel}
	 * @param operation      the {@link Contactor} operation
	 * @param relay          is the relay NORMALLY_OPEN or NORMALLY_CLOSED?
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void setOutput(ChannelAddress channelAddress, Contactor operation, Relay relay)
			throws IllegalArgumentException, OpenemsNamedException {
		BooleanWriteChannel channel = this.componentManager.getChannel(channelAddress);

		// Get Target Value
		Boolean targetValue = null;
		switch (operation) {
		case OPEN:
			switch (relay) {
			case NORMALLY_CLOSED:
				targetValue = true;
				break;
			case NORMALLY_OPEN:
				targetValue = false;
				break;
			}
			break;
		case CLOSE:
			switch (relay) {
			case NORMALLY_CLOSED:
				targetValue = false;
				break;
			case NORMALLY_OPEN:
				targetValue = true;
				break;
			}
			break;
		}

		if (Objects.equal(channel.value().get(), targetValue)) {
			// it is already in the desired state
		} else {
			channel.setNextWriteValue(targetValue);
		}
	}
}
