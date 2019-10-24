package io.openems.edge.meter.abb.b32;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.bridge.mbus.api.task.MbusTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * @author Leonid Verhovskij
 * 
 *         Standard VIF codes
 * 
 *         VIF-code Description Range coding Range E000 0nnn Energy 10( nnn-3 )
 *         Wh 0.001Wh to 10000Wh E010 1nnn Power 10( nnn-3 ) W 0.001W to 10000W
 *         E111 1000 Fabrication No. 00000000 to 99999999 E111 1010 Bus address
 *         0–250 1111 1011 Extension of VIF-codes Not used by the meter 1111
 *         1101 Extension of VIF-codes True VIF is given in the first VIFE and
 *         is coded using Table FD 1111 1111 Manufacturer specific Next VIFE is
 *         manufacturer specific
 * 
 *         First manufacturer specific VIFE-codes VIFE-code Description
 * 
 *         E000 0000 Total E000 0001 L1 E000 0010 L2 E000 0011 L3 E000 0100 N
 *         E000 0101 L1-L2 E000 0110 L3-L2 E000 0111 L1 - L3 E001 0000 Pulse
 *         frequency ... See 2CMC485003M0201_A_en_B23_B24_User_Manual_002
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.meter.abb", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterABBB23Mbus extends AbstractOpenemsMbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.GRID;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BridgeMbus mbus;

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

	public MeterABBB23Mbus() {
		super(OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus",
				config.mbus_id());
		//register into mbus bridge task list
		this.mbus.addTask(config.id(), new MbusTask(this.mbus, this));
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
		channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.TOTAL_CONSUMED_ENERGY), 0));
		channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1), 1));
		channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2), 2));
		channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 3));
		channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 4));
		channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 5));
		channelDataRecordsList.add(new ChannelRecord(channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), 6));
		channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), DataType.Manufacturer));
		channelDataRecordsList.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), DataType.DeviceId));
	}

}
