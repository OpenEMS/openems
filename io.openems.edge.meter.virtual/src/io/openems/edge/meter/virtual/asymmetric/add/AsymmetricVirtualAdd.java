package io.openems.edge.meter.virtual.asymmetric.add;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.meter.virtual.common.AbstractVirtualAddMeter;
import io.openems.edge.meter.virtual.symmetric.add.SymmetricChannelManager;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Asymmetric.Add", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class AsymmetricVirtualAdd extends AbstractVirtualAddMeter<AsymmetricMeter>
		implements VirtualMeter, AsymmetricMeter, SymmetricMeter, OpenemsComponent, ModbusSlave {

	private final AsymmetricChannelManager channelManager = new AsymmetricChannelManager(this);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	protected List<AsymmetricMeter> meters;

	private Config config;

	public AsymmetricVirtualAdd() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values() //
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
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(AsymmetricVirtualAdd.class, accessMode, 100) //
						.build());
	}

	@Override
	protected List<AsymmetricMeter> getMeters() {
		return this.meters;
	}

	@Override
	protected SymmetricChannelManager getChannelManager() {
		return this.channelManager;
	}

}
