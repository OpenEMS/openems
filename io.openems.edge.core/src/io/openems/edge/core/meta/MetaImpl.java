package io.openems.edge.core.meta;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.AccessMode;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Meta.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
public class MetaImpl extends AbstractOpenemsComponent implements Meta, OpenemsComponent, ModbusSlave {

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private OpenemsEdgeOem oem;

	public MetaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values() //
		);
		this.channel(Meta.ChannelId.VERSION).setNextValue(OpenemsConstants.VERSION.toString());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, true);

		// Update the Channel _meta/SystemTimeUtc after every second
		final var systemTimeUtcChannel = this.<LongReadChannel>channel(Meta.ChannelId.SYSTEM_TIME_UTC);
		this.executor.scheduleAtFixedRate(() -> {
			systemTimeUtcChannel.setNextValue(Instant.now().getEpochSecond());
		}, 0, 1000, TimeUnit.MILLISECONDS);

		this.applyConfig(config);
		if (OpenemsComponent.validateSingleton(this.cm, Meta.SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, true);

		this.applyConfig(config);
		if (OpenemsComponent.validateSingleton(this.cm, Meta.SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		shutdownAndAwaitTermination(this.executor, 0);
		super.deactivate();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return Meta.getModbusSlaveTable(accessMode, this.oem);
	}

	private void applyConfig(Config config) {
		this._setCurrency(config.currency().toCurrency());
	}
}
