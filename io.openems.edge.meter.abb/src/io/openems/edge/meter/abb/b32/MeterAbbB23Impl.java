package io.openems.edge.meter.abb.b32;

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

import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.bridge.mbus.api.MbusTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.ABB.B23", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterAbbB23Impl extends AbstractOpenemsMbusComponent
		implements MeterAbbB23, ElectricityMeter, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private BridgeMbus mbus;

	private MeterType meterType = MeterType.PRODUCTION;

	public MeterAbbB23Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterAbbB23.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
				config.mbus_id());
		// register into mbus bridge task list
		this.mbus.addTask(config.id(), new MbusTask(this.mbus, this));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected void addChannelDataRecords() {
		this.channelDataRecordsList
				.add(new ChannelRecord(this.channel(MeterAbbB23.ChannelId.TOTAL_CONSUMED_ENERGY), 0));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1), 1));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2), 2));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3), 3));
		// TODO mapping seems to be wrong; L3 is repeated
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3), 4));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3), 5));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3), 6));
		this.channelDataRecordsList
				.add(new ChannelRecord(this.channel(MeterAbbB23.ChannelId.MANUFACTURER_ID), DataType.Manufacturer));
		this.channelDataRecordsList
				.add(new ChannelRecord(this.channel(MeterAbbB23.ChannelId.DEVICE_ID), DataType.DeviceId));
	}

}
