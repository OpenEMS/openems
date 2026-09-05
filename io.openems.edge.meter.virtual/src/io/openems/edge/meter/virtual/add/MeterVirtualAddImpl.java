package io.openems.edge.meter.virtual.add;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.SumOptions;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Virtual.Add", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@GenerateTargetsFromReferences("Meter")
public class MeterVirtualAddImpl extends AbstractOpenemsComponent
		implements MeterVirtualAdd, ElectricityMeter, OpenemsComponent, ModbusSlave, SumOptions {

	private final AddChannelManager channelManager = new AddChannelManager(this);
	private final List<ElectricityMeter> meters = new ArrayList<>();

	@Reference(name = "Meter", policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, //
			target = "(&(id=${config.meterIds})(enabled=true))")
	protected void addMeter(ElectricityMeter meter) {
		synchronized (this.meters) {
			this.meters.add(meter);
			this.channelManager.update(this.meters);
		}
	}

	protected void removeMeter(ElectricityMeter meter) {
		synchronized (this.meters) {
			this.meters.remove(meter);
			this.channelManager.update(this.meters);
		}
	}

	private Config config;

	public MeterVirtualAddImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterVirtualAdd.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.channelManager.update(this.meters);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.channelManager.deactivate();
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
	public String debugLog() {
		return this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode));
	}

}
