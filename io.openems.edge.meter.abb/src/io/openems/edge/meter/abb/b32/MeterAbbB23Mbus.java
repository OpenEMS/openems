package io.openems.edge.meter.abb.b32;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import org.openmuc.jmbus.VariableDataStructure;
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

import java.util.List;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.ABB.B23", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)

public class MeterAbbB23Mbus extends AbstractOpenemsMbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMbus(BridgeMbus mbus) {
		super.setMbus(mbus);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TOTAL_CONSUMED_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		DEVICE_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public MeterAbbB23Mbus() {
		super(OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
				config.mbus_id(), 0);
	}

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
		this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.TOTAL_CONSUMED_ENERGY), 0));
		this.channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1), 1));
		this.channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2), 2));
		this.channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 3));
		// TODO mapping seems to be wrong; L3 is repeated
		this.channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 4));
		this.channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 5));
		this.channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 6));
		this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), DataType.Manufacturer));
		this.channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), DataType.DeviceId));
	}

	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_CONSUMED_ENERGY}.
	 *
	 * @return the Channel
	 */
	public Channel<Integer> getTotalConsumedEnergyChannel() {
		return this.channel(ChannelId.TOTAL_CONSUMED_ENERGY);
	}

	/**
	 * Gets the Total Consumed Energy in [kWh]. See {@link ChannelId#TOTAL_CONSUMED_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public Value<Integer> getTotalConsumedEnergy() {
		return this.getTotalConsumedEnergyChannel().value();
	}

	@Override
	public String debugLog() {
		String debug = "";
		if (this.getActivePower().isDefined()) {
			debug += this.getActivePowerChannel().channelId().id() + " "
					+ this.getActivePower().toString()
					+ "\n";
		}
		if (this.getActiveConsumptionEnergy().isDefined()) {
			debug += this.getActiveConsumptionEnergyChannel().channelId().id()
					+ " " + getActiveConsumptionEnergy().toString()
					+ "\n";
		}
		if (this.getActiveProductionEnergy().isDefined()) {
			debug += this.getActiveProductionEnergyChannel().channelId().id()
					+ " " + getActiveProductionEnergy().toString()
					+ "\n";
		}
		if (this.getTotalConsumedEnergy().isDefined()) {
			debug += this.getTotalConsumedEnergyChannel().channelId().id()
					+ " " + this.getTotalConsumedEnergy().toString()
					+ "\n";
		}
		return debug;
	}

	@Override
	public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
		// Not needed so far.
	}

}