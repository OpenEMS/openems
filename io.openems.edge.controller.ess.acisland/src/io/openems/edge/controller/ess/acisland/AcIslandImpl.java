package io.openems.edge.controller.ess.acisland;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.AcIsland", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AcIslandImpl extends AbstractOpenemsComponent implements AcIsland, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AcIslandImpl.class);

	@Reference
	private ComponentManager componentManager;

	public AcIslandImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				AcIsland.ChannelId.values() //
		);
	}

	private Config config;
	/** The current state in the State Machine. */
	private State state = State.UNDEFINED;
	private ChannelAddress offGridOutputChannelAddress;
	private ChannelAddress onGridOutputChannelAddress;
	private boolean isProducerDisconnected = false;
	private long timeProducerDisconnected;

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.offGridOutputChannelAddress = ChannelAddress.fromString(config.offGridOutputChannelAddress());
		this.onGridOutputChannelAddress = ChannelAddress.fromString(config.onGridOutputChannelAddress());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Get all required values - or abort with exception
		SymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		var gridMode = ess.getGridMode();
		int soc = ess.getSoc().getOrError();

		BooleanWriteChannel onGridOutputChannel = this.componentManager.getChannel(this.onGridOutputChannelAddress);
		BooleanWriteChannel offGridOutputChannel = this.componentManager.getChannel(this.offGridOutputChannelAddress);
		var isProducerOffGrid = this.isProducerOffGrid(onGridOutputChannel, offGridOutputChannel);
		var isProducerOff = this.isProducerOff(onGridOutputChannel, offGridOutputChannel);
		var isProducerOnGrid = this.isProducerOnGrid(onGridOutputChannel, offGridOutputChannel);

		/*
		 * State Machine
		 */
		boolean stateChanged;

		do {
			stateChanged = false;
			switch (this.state) {
			case OFF_GRID: {
				if (isProducerOffGrid || isProducerOff) {
					if (gridMode == GridMode.ON_GRID) {
						stateChanged = this.changeState(State.SWITCH_TO_ONGRID);
					} else if (soc >= this.config.maxSoc()) {
						this.disconnectOffGrid(offGridOutputChannel);
					} else if (soc <= this.config.minSoc()) {
						this.connectOffGrid(offGridOutputChannel);
					}
				} else {
					stateChanged = this.changeState(State.SWITCH_TO_OFFGRID);
				}
				break;
			}

			case ON_GRID: {
				if (isProducerOnGrid) {
					if (gridMode == GridMode.OFF_GRID) {
						stateChanged = this.changeState(State.SWITCH_TO_OFFGRID);
					}
				} else {
					stateChanged = this.changeState(State.SWITCH_TO_ONGRID);
				}
				break;
			}

			case SWITCH_TO_OFFGRID: {
				if (isProducerOff) {
					if (!this.isProducerDisconnected) {
						this.isProducerDisconnected = true;
						this.timeProducerDisconnected = System.currentTimeMillis();
					}
					if (this.timeProducerDisconnected + this.config.switchDelay() <= System.currentTimeMillis()
							&& this.isProducerDisconnected) {
						stateChanged = this.changeState(State.OFF_GRID);
					}
				} else {
					this.isProducerDisconnected = false;
					this.disconnectOnGrid(onGridOutputChannel);
					this.disconnectOffGrid(offGridOutputChannel);
				}
				break;
			}

			case SWITCH_TO_ONGRID: {
				if (isProducerOnGrid) {
					this.changeState(State.ON_GRID);
					this.isProducerDisconnected = false;
				} else if (isProducerOff) {
					if (!this.isProducerDisconnected) {
						this.isProducerDisconnected = true;
						this.timeProducerDisconnected = System.currentTimeMillis();
					}
					if (this.timeProducerDisconnected + this.config.switchDelay() <= System.currentTimeMillis()
							&& this.isProducerDisconnected) {
						this.connectOnGrid(onGridOutputChannel);
					}
				} else {
					this.isProducerDisconnected = false;
					this.disconnectOnGrid(onGridOutputChannel);
					this.disconnectOffGrid(offGridOutputChannel);
				}
				break;
			}

			default:
				if (gridMode == GridMode.ON_GRID) {
					stateChanged = this.changeState(State.SWITCH_TO_ONGRID);
				} else if (gridMode == GridMode.OFF_GRID) {
					stateChanged = this.changeState(State.SWITCH_TO_OFFGRID);
				}
			}
		} while (stateChanged); // execute again if the state changed

		// store current state in StateMachine channel
		this.channel(AcIsland.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	private boolean isProducerOff(BooleanWriteChannel onGridOutputChannel, BooleanWriteChannel offGridOutputChannel) {
		return this.isOnGridOn(onGridOutputChannel) == false && this.isOffGridOn(offGridOutputChannel) == false;
	}

	private boolean isProducerOffGrid(BooleanWriteChannel onGridOutputChannel,
			BooleanWriteChannel offGridOutputChannel) {
		return this.isOnGridOn(onGridOutputChannel) == false && this.isOffGridOn(offGridOutputChannel) == true;
	}

	private boolean isProducerOnGrid(BooleanWriteChannel onGridOutputChannel,
			BooleanWriteChannel offGridOutputChannel) {
		return this.isOnGridOn(onGridOutputChannel) == true && this.isOffGridOn(offGridOutputChannel) == false;
	}

	private void connectOnGrid(BooleanWriteChannel onGridOutputChannel) throws OpenemsNamedException {
		this.switchOutput(onGridOutputChannel, true, this.config.invertOnGridOutput());
	}

	private void disconnectOnGrid(BooleanWriteChannel onGridOutputChannel) throws OpenemsNamedException {
		this.switchOutput(onGridOutputChannel, false, this.config.invertOnGridOutput());
	}

	private boolean isOnGridOn(BooleanWriteChannel onGridOutputChannel) {
		return onGridOutputChannel.value().orElse(false) ^ this.config.invertOnGridOutput();
	}

	private void connectOffGrid(BooleanWriteChannel offGridOutputChannel) throws OpenemsNamedException {
		this.switchOutput(offGridOutputChannel, true, this.config.invertOffGridOutput());
	}

	private void disconnectOffGrid(BooleanWriteChannel offGridOutputChannel) throws OpenemsNamedException {
		this.switchOutput(offGridOutputChannel, false, this.config.invertOffGridOutput());
	}

	private boolean isOffGridOn(BooleanWriteChannel offGridOutputChannel) {
		return offGridOutputChannel.value().orElse(false) ^ this.config.invertOffGridOutput();
	}

	private void switchOutput(BooleanWriteChannel outputChannel, boolean on, boolean invertOutput)
			throws OpenemsNamedException {
		var currentValueOpt = outputChannel.value().asOptional();
		if (!currentValueOpt.isPresent() || currentValueOpt.get() != (on ^ invertOutput)) {
			this.logInfo(this.log,
					"Set output [" + outputChannel.address() + "] " + (on ^ invertOutput ? "ON" : "OFF") + ".");
			outputChannel.setNextWriteValue(on ^ invertOutput);
		}
	}

	/**
	 * A flag to maintain change in the state.
	 *
	 * @param nextState the target state
	 * @return Flag that the state is changed or not
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			this.state = nextState;
			return true;
		}
		return false;
	}

}
