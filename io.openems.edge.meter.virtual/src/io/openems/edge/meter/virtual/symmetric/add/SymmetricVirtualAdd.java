package io.openems.edge.meter.virtual.symmetric.add;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
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

	private final SymmetricChannelManager channelManager = new SymmetricChannelManager(this);
	private final List<SymmetricMeter> meters = new CopyOnWriteArrayList<>();

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Meter.Virtual.Symmetric.Add)))")

	protected synchronized void addMeter(SymmetricMeter meter) {
		this.meters.add(meter);
		this.channelManager.deactivate();
		this.channelManager.activate(this.meters);
	}

	protected synchronized void removeMeter(SymmetricMeter meter) {
		this.meters.remove(meter);
		this.channelManager.deactivate();
		this.channelManager.activate(this.meters);
	}

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
		this.channelManager.activate(this.meters);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.channelManager.deactivate();
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public boolean addToSum() {
		return this.config.addToSum();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected List<SymmetricMeter> getMeters() {
		return this.meters;
	}

}
