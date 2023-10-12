package io.openems.edge.meter.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

public abstract class AbstractDummyElectricityMeter<SELF extends AbstractDummyElectricityMeter<?>>
		extends AbstractOpenemsComponent implements ElectricityMeter {

	private MeterType meterType;

	protected AbstractDummyElectricityMeter(String id,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	protected abstract SELF self();

	/**
	 * Set the {@link MeterType}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withMeterType(MeterType meterType) {
		this.meterType = meterType;
		return this.self();
	}

	/**
	 * Set {@link SymmetricEss.ChannelId#ACTIVE_POWER} of this
	 * {@link ElectricityMeter}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public SELF withActivePower(int value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, value);
		return this.self();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

}
