package io.openems.edge.ess.byd.container.watchdog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
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
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BydContainerWatchdog extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(BydContainerWatchdog.class);

	@Reference
	protected ComponentManager componentManager;
	
	@Reference
	protected ConfigurationAdmin cm;

	private Config config;

	public BydContainerWatchdog() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		WATCHDOG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws IllegalArgumentException, OpenemsNamedException {
		// Get ESS
		EssFeneconBydContainer ess = this.componentManager.getComponent(this.config.ess_id());
		boolean isReadonly = (boolean) ess.getComponentContext().getProperties().get("readonly");

		// Check if Watchdog has been triggered in time. Timeout is configured in
		// Modbus-TCP-Api Controller.
		IntegerWriteChannel channel = this.channel(ChannelId.WATCHDOG);
		Optional<Integer> value = channel.getNextWriteValueAndReset();

		if (value.isPresent()) {
			/*
			 * No Timeout
			 */
			if (isReadonly) {
				// Everything ok
			} else {
				// Set to read-only mode
//				List<UpdateComponentConfigRequest.Property> properties = new ArrayList<>();
//				properties.add(new UpdateComponentConfigRequest.Property("readonly", new JsonPrimitive(true)));
//				UpdateComponentConfigRequest request = new UpdateComponentConfigRequest(this.config.ess_id(),
//						properties);
//				this.componentManager.handleJsonrpcRequest(, request)

//				OpenemsComponent.updateConfigurationProperty(this.cm, ess.get, property, value);
			}

		} else {
			// Timeout happened

		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(BydContainerWatchdog.class, accessMode, 100) //
						.channel(0, ChannelId.WATCHDOG, ModbusType.UINT16) //
						.build());
	}
}
