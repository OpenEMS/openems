package io.openems.edge.meter.virtual.add;

import java.util.ArrayList;
import java.util.List;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.VirtualMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Add", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class VirtualAddMeterImpl extends AbstractOpenemsComponent
		implements VirtualAddMeter, VirtualMeter, ElectricityMeter, OpenemsComponent, ModbusSlave {

	private final AddChannelManager channelManager = new AddChannelManager(this);
	private final List<ElectricityMeter> meters = new ArrayList<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE)
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

	public VirtualAddMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				VirtualAddMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Meter", config.meterIds())) {
			return;
		}

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
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(VirtualAddMeterImpl.class, accessMode, 100) //
						.build());
	}

}
