package io.openems.edge.ess.byd.container.watchdog;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.byd.container.EssFeneconBydContainer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Fenecon.BydContainer.WatchdogController", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssFeneconBydContainerWatchdogControllerImpl extends AbstractOpenemsComponent
		implements EssFeneconBydContainerWatchdogController, Controller, OpenemsComponent, ModbusSlave {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	private Config config;

	public EssFeneconBydContainerWatchdogControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EssFeneconBydContainerWatchdogController.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		// Get ESS
		EssFeneconBydContainer ess = this.componentManager.getComponent(this.config.ess_id());
		IntegerWriteChannel channel = this.channel(EssFeneconBydContainerWatchdogController.ChannelId.WATCHDOG);
		var isReadonly = (boolean) ess.getComponentContext().getProperties().get("readonly");
		var value = channel.getNextWriteValueAndReset();

		// Check if Watchdog has been triggered in time.
		// Timeout is configured in Modbus-TCP-Api Controller.
		if (value.isPresent()) {
			// No Timeout

			if (isReadonly) {
				// if readonly is already set to true --> do nothing
			} else {
				// Set to read-only mode
				this.setConfig(true, ess.servicePid());
			}
		} else if (isReadonly) {
			// Timeout happened, Set readonly flag to false once.
			this.setConfig(false, ess.servicePid());

		} else {
			// setting the active and reactive power to zero
			ess.setActivePowerEquals(0);
			ess.setReactivePowerEquals(0);
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssFeneconBydContainerWatchdogControllerImpl.class, accessMode, 100) //
						.channel(0, EssFeneconBydContainerWatchdogController.ChannelId.WATCHDOG, ModbusType.UINT16) //
						.build());
	}

	/**
	 * Helper function to set the configuration based on the watchdog value.
	 *
	 * @param value true to set readonly flag on, false to set the readonly flag
	 *              off;
	 * @param pid   pid of the Ess
	 * @throws OpenemsNamedException on error
	 *
	 */
	private void setConfig(Boolean value, String pid) throws OpenemsNamedException {
		OpenemsComponent.updateConfigurationProperty(this.cm, pid, "readonly", value);
	}
}
