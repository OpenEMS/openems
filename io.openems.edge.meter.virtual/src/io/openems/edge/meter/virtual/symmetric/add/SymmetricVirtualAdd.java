package io.openems.edge.meter.virtual.symmetric.add;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.meter.virtual.asymmetric.add.AsymmetricChannelManager;
import io.openems.edge.meter.virtual.common.AbstractVirtualAddMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Symmetric.Add", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SymmetricVirtualAdd extends AbstractVirtualAddMeter<SymmetricMeter>
		implements VirtualMeter, SymmetricMeter, OpenemsComponent, ModbusSlave {

	private final AsymmetricChannelManager channelManager = new AsymmetricChannelManager(this);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	protected List<SymmetricMeter> meters;

	private Config config;

	public SymmetricVirtualAdd() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.configurationAdmin,
				config.meterIds());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public boolean addToSum() {
		return this.config.addToSum();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SymmetricVirtualAdd.class, accessMode, 100) //
						.build());
	}

	@Override
	protected List<SymmetricMeter> getMeters() {
		return this.meters;
	}

	@Override
	protected SymmetricChannelManager getChannelManager() {
		return this.channelManager;
	}

}
