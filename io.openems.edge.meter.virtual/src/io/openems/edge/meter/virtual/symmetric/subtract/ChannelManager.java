package io.openems.edge.meter.virtual.symmetric.subtract;

import java.util.List;
import java.util.function.BiConsumer;

import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

public class ChannelManager extends AbstractChannelListenerManager {

	private final VirtualSubtractMeter parent;

	public ChannelManager(VirtualSubtractMeter parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 *
	 * @param minuend     the Minuend Component
	 * @param subtrahends the Subtrahend Components
	 */
	protected void activate(OpenemsComponent minuend, List<OpenemsComponent> subtrahends) {
		// Minuend
		if (minuend instanceof SymmetricMeter || minuend instanceof SymmetricEss) {
			// OK
		} else {
			throw new IllegalArgumentException("Minuend [" + minuend.id() + "] is neither a Meter nor a ESS");
		}

		// Subtrahends
		for (OpenemsComponent subtrahend : subtrahends) {
			if (subtrahend instanceof SymmetricMeter || subtrahend instanceof SymmetricEss) {
				// OK
			} else {
				throw new IllegalArgumentException("Subtrahend [" + subtrahend.id() + "] is neither a Meter nor a ESS");
			}
		}

		this.activateSubtractInteger(minuend, subtrahends, SymmetricMeter.ChannelId.ACTIVE_POWER,
				SymmetricEss.ChannelId.ACTIVE_POWER);
		this.activateSubtractInteger(minuend, subtrahends, SymmetricMeter.ChannelId.REACTIVE_POWER,
				SymmetricEss.ChannelId.REACTIVE_POWER);

		this.activateSubtractLong(minuend, subtrahends, SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
				SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
		this.activateSubtractLong(minuend, subtrahends, SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
				SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	}

	private void activateSubtractInteger(OpenemsComponent minuend, List<OpenemsComponent> subtrahends,
			SymmetricMeter.ChannelId meterChannelId, SymmetricEss.ChannelId essChannelId) {
		final BiConsumer<Value<Integer>, Value<Integer>> callback = (oldValue, newValue) -> {
			Integer result = null;

			// Minuend
			if (minuend instanceof SymmetricMeter) {
				IntegerReadChannel channel = ((SymmetricMeter) minuend).channel(meterChannelId);
				result = channel.getNextValue().get();
			} else if (minuend instanceof SymmetricEss) {
				IntegerReadChannel channel = ((SymmetricEss) minuend).channel(essChannelId);
				result = channel.getNextValue().get();
			}

			// Subtrahends
			for (OpenemsComponent subtrahend : subtrahends) {
				if (subtrahend instanceof SymmetricMeter) {
					IntegerReadChannel channel = ((SymmetricMeter) subtrahend).channel(meterChannelId);
					result = TypeUtils.subtract(result, channel.getNextValue().get());
				} else if (subtrahend instanceof SymmetricEss) {
					IntegerReadChannel channel = ((SymmetricEss) subtrahend).channel(essChannelId);
					result = TypeUtils.subtract(result, channel.getNextValue().get());
				}
			}

			IntegerReadChannel channel = this.parent.channel(meterChannelId);
			channel.setNextValue(result);
		};

		// Minuend
		if (minuend instanceof SymmetricMeter) {
			this.addOnChangeListener(minuend, meterChannelId, callback);
		} else if (minuend instanceof SymmetricEss) {
			this.addOnChangeListener(minuend, essChannelId, callback);
		}

		// Subtrahends
		for (OpenemsComponent subtrahend : subtrahends) {
			if (subtrahend instanceof SymmetricMeter) {
				this.addOnChangeListener(subtrahend, meterChannelId, callback);
			} else if (subtrahend instanceof SymmetricEss) {
				this.addOnChangeListener(subtrahend, essChannelId, callback);
			}
		}
	}

	private void activateSubtractLong(OpenemsComponent minuend, List<OpenemsComponent> subtrahends,
			SymmetricMeter.ChannelId meterChannelId, SymmetricEss.ChannelId essChannelId) {
		final BiConsumer<Value<Long>, Value<Long>> callback = (oldValue, newValue) -> {
			Long result = null;

			// Minuend
			if (minuend instanceof SymmetricMeter) {
				LongReadChannel channel = ((SymmetricMeter) minuend).channel(meterChannelId);
				result = channel.getNextValue().get();
			} else if (minuend instanceof SymmetricEss) {
				LongReadChannel channel = ((SymmetricEss) minuend).channel(essChannelId);
				result = channel.getNextValue().get();
			}

			// Subtrahends
			for (OpenemsComponent subtrahend : subtrahends) {
				if (subtrahend instanceof SymmetricMeter) {
					LongReadChannel channel = ((SymmetricMeter) subtrahend).channel(meterChannelId);
					result = TypeUtils.subtract(result, channel.getNextValue().get());
				} else if (subtrahend instanceof SymmetricEss) {
					LongReadChannel channel = ((SymmetricEss) subtrahend).channel(essChannelId);
					result = TypeUtils.subtract(result, channel.getNextValue().get());
				}
			}

			LongReadChannel channel = this.parent.channel(meterChannelId);
			channel.setNextValue(result);
		};

		// Minuend
		if (minuend instanceof SymmetricMeter) {
			this.addOnChangeListener(minuend, meterChannelId, callback);
		} else if (minuend instanceof SymmetricEss) {
			this.addOnChangeListener(minuend, essChannelId, callback);
		}

		// Subtrahends
		for (OpenemsComponent subtrahend : subtrahends) {
			if (subtrahend instanceof SymmetricMeter) {
				this.addOnChangeListener(subtrahend, meterChannelId, callback);
			} else if (subtrahend instanceof SymmetricEss) {
				this.addOnChangeListener(subtrahend, essChannelId, callback);
			}
		}
	}

}
