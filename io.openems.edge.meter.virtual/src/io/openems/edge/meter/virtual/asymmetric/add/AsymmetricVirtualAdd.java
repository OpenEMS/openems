package io.openems.edge.meter.virtual.asymmetric.add;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.meter.virtual.common.AbstractVirtualAddMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Asymmetric.Add", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class AsymmetricVirtualAdd extends AbstractVirtualAddMeter<AsymmetricMeter>
		implements VirtualMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	private final AsymmetricChannelManager channelManager = new AsymmetricChannelManager(this);
	private final List<AsymmetricMeter> meters = new CopyOnWriteArrayList<>();

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Meter.Virtual.Asymmetric.Add)))")
	protected synchronized void addMeter(AsymmetricMeter meter) {
		this.meters.add(meter);
		this.channelManager.deactivate();
		this.channelManager.activate(this.meters);
	}

	protected synchronized void removeMeter(AsymmetricMeter meter) {
		this.meters.remove(meter);
		this.channelManager.deactivate();
		this.channelManager.activate(this.meters);
	}

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
		if (OpenemsComponent.updateReferenceFilter(this.configurationAdmin, this.servicePid(), "Meter",
				config.meterIds())) {
			return;
		}
		// check the meter type
		this.checkMeterType(config.type());
		this.config = config;
		this.channelManager.activate(this.meters);
	}

	/**
	 * Throws exception if the meter-type is production.
	 * 
	 * @param type the {@link MeterType}
	 * @throws OpenemsNamedException on error.
	 */
	private void checkMeterType(MeterType type) throws OpenemsNamedException {
		if (type == MeterType.PRODUCTION) {
			throw new OpenemsException(
					"Virtual asymmetric add meter is never a Production meter, check the type of meter in configuration");
		}
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
		return "L:" + this.getActivePower().asString() //
				+ "|L1: " + this.getActivePowerL1().asString() //
				+ "|L2: " + this.getActivePowerL2().asString() //
				+ "|L3: " + this.getActivePowerL3().asString();
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
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected List<AsymmetricMeter> getMeters() {
		return this.meters;
	}
}
