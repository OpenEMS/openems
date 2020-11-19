package io.openems.edge.ess.sinexcel;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.AbstractChannelListenerManager;

public class ChannelManager extends AbstractChannelListenerManager {

	private final EssSinexcel parent;

	public ChannelManager(EssSinexcel parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 * 
	 * @param battery the {@link Battery}
	 */
	public void activate(Battery battery) {
		this.<Integer>addOnChangeListener(battery, Battery.ChannelId.SOC, (oldValue, newValue) -> {
			this.parent._setSoc(newValue.get());
			this.parent.channel(EssSinexcel.ChannelId.BAT_SOC).setNextValue(newValue.get());
		});
		this.<Integer>addOnChangeListener(battery, Battery.ChannelId.VOLTAGE, (oldValue, newValue) -> {
			this.parent.channel(EssSinexcel.ChannelId.BAT_VOLTAGE).setNextValue(newValue.get());
		});
		this.<Integer>addOnChangeListener(battery, Battery.ChannelId.MIN_CELL_VOLTAGE, (oldValue, newValue) -> {
			this.parent._setMinCellVoltage(newValue.get());
		});
		this.<Integer>addOnChangeListener(battery, Battery.ChannelId.CAPACITY, (oldValue, newValue) -> {
			this.parent._setCapacity(newValue.get());
		});
	}

}
