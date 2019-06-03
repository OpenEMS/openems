package io.openems.edge.project.controller.karpfsee.emergencymode;

import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.EmergencyMode", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EmergencyMode extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private Config config;
	private ChannelAddress onGridIndicationController;
	private ChannelAddress offGridIndicationController;
	private ChannelAddress blockHeatPowerPlantPermissionSignal;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	public EmergencyMode() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ThisChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.onGridIndicationController = ChannelAddress.fromString(config.onGridIndicationController());
		this.offGridIndicationController = ChannelAddress.fromString(config.offGridIndicationController());
		this.blockHeatPowerPlantPermissionSignal = ChannelAddress
				.fromString(config.blockHeatPowerPlantPermissionSignal());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		switch (this.getGridMode()) {
		case UNDEFINED:
			/*
			 * Grid-Mode is undefined -> wait till we have some clear information
			 */
			break;
		case OFF_GRID:
			this.handleOffGridState();
			break;

		case ON_GRID:
//			this.handleOnGridState();
			break;
		}
	}

	private void handleOnGridState() throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.CLOSE);
		this.setOutput(this.onGridIndicationController, Operation.CLOSE);
		this.setOutput(this.offGridIndicationController, Operation.OPEN);
	}

	private void handleOffGridState() throws IllegalArgumentException, OpenemsNamedException {
		ManagedSymmetricEss ess;
		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			throw new OpenemsException("Unable to get the component [" + this.config.ess_id() + "] " + e.getMessage());
		}
		Optional<Integer> soc = ess.getSoc().value().asOptional();
		if (soc.isPresent()) {
			if (soc.get() < 95) {
//				this.setOutput(this.blockHeatPowerPlantPermissionSignal, Operation.CLOSE);
			} else {

			}
		}
//		this.setOutput(this.onGridIndicationController, Operation.CLOSE);
//		this.setOutput(this.offGridIndicationController, Operation.OPEN);
	}

	/**
	 * Gets the Grid-Mode of ESS.
	 * 
	 * @return the Grid-Mode
	 */
	private GridMode getGridMode() {
		SymmetricEss ess;
		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			return GridMode.UNDEFINED;
		}
		GridMode essGridMode = ess.getGridMode().value().asEnum();
		if ((essGridMode == GridMode.ON_GRID)) {
			return GridMode.ON_GRID;
		} else if ((essGridMode == GridMode.OFF_GRID)) {
			return GridMode.OFF_GRID;
		} else {
			return GridMode.UNDEFINED;
		}
	}

	/**
	 * Set Switch to Close or Open Operation.
	 * 
	 * @param channelAddress the Address of the BooleanWriteChannel
	 * @param operation      Close --> Make line connection; <br/>
	 *                       Open --> Make line disconnection
	 * @return true if the output was actually switched; false if it had already
	 *         been in the desired state
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private boolean setOutput(ChannelAddress channelAddress, Operation operation)
			throws IllegalArgumentException, OpenemsNamedException {
		boolean switchedOutput = false;
		BooleanWriteChannel channel = this.componentManager.getChannel(channelAddress);
		switch (operation) {
		case CLOSE:
			switchedOutput = this.setOutput(channel, true);
			break;
		case OPEN:
			switchedOutput = this.setOutput(channel, false);
			break;
		case UNDEFINED:
			break;
		}
		return switchedOutput;
	}

	/**
	 * Sets the Output.
	 * 
	 * @param channel the BooleanWriteChannel
	 * @param value   true to set the output, false to unset it
	 * @return true if the output was actually switched; false if it had already
	 *         been in the desired state
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private boolean setOutput(BooleanWriteChannel channel, boolean value)
			throws IllegalArgumentException, OpenemsNamedException {
		if (channel.value().asOptional().equals(Optional.of(value))) {
			// it is already in the desired state
			return false;
		} else {
			channel.setNextWriteValue(value);
			return true;
		}
	}
}
