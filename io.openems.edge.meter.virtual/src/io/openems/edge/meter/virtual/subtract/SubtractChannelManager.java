package io.openems.edge.meter.virtual.subtract;

import java.util.List;
import java.util.function.Consumer;

import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

public class SubtractChannelManager extends AbstractChannelListenerManager {

	private final VirtualSubtractMeter parent;

	public SubtractChannelManager(VirtualSubtractMeter parent) {
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
		if (minuend == null || minuend instanceof ElectricityMeter || minuend instanceof SymmetricEss) {
			// OK
		} else {
			throw new IllegalArgumentException("Minuend [" + minuend.id() + "] is neither a Meter nor a ESS");
		}

		// Subtrahends
		for (OpenemsComponent subtrahend : subtrahends) {
			if (subtrahend instanceof ElectricityMeter || subtrahend instanceof SymmetricEss) {
				// OK
			} else {
				throw new IllegalArgumentException("Subtrahend [" + subtrahend.id() + "] is neither a Meter nor a ESS");
			}
		}

		this.activateSubtractInteger(minuend, subtrahends, ElectricityMeter.ChannelId.ACTIVE_POWER,
				SymmetricEss.ChannelId.ACTIVE_POWER);
		this.activateSubtractInteger(minuend, subtrahends, ElectricityMeter.ChannelId.REACTIVE_POWER,
				SymmetricEss.ChannelId.REACTIVE_POWER);

		this.activateSubtractLong(minuend, subtrahends, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
				SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
		this.activateSubtractLong(minuend, subtrahends, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
				SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	}

	private void activateSubtractInteger(OpenemsComponent minuend /* nullable */, List<OpenemsComponent> subtrahends,
			ElectricityMeter.ChannelId meterChannelId, SymmetricEss.ChannelId essChannelId) {
		final Consumer<Value<Integer>> callback = (ignore) -> {
			// Subtrahends
			Integer subtrahendsSum = null;
			for (OpenemsComponent subtrahend : subtrahends) {
				if (subtrahend instanceof ElectricityMeter meter) {
					IntegerReadChannel channel = meter.channel(meterChannelId);
					subtrahendsSum = TypeUtils.sum(subtrahendsSum, channel.getNextValue().get());
				} else if (subtrahend instanceof SymmetricEss) {
					IntegerReadChannel channel = ((SymmetricEss) subtrahend).channel(essChannelId);
					subtrahendsSum = TypeUtils.sum(subtrahendsSum, channel.getNextValue().get());
				}
			}

			final Integer minuendValue;
			if (minuend == null) {
				minuendValue = 0;
			} else if (minuend instanceof ElectricityMeter meter) {
				IntegerReadChannel channel = meter.channel(meterChannelId);
				minuendValue = channel.getNextValue().get();
			} else if (minuend instanceof SymmetricEss) {
				IntegerReadChannel channel = ((SymmetricEss) minuend).channel(essChannelId);
				minuendValue = channel.getNextValue().get();
			} else {
				// should not happen
				minuendValue = null;
			}

			final Integer result;
			// Minuend
			if (minuend == null && subtrahendsSum == null) {
				result = null;
			} else {
				result = TypeUtils.subtract(minuendValue, subtrahendsSum);
			}

			IntegerReadChannel channel = this.parent.channel(meterChannelId);
			channel.setNextValue(result);
		};

		// Minuend
		if (minuend == null) {
			// no listener for minuend
		} else if (minuend instanceof ElectricityMeter) {
			this.addOnSetNextValueListener(minuend, meterChannelId, callback);
		} else if (minuend instanceof SymmetricEss) {
			this.addOnSetNextValueListener(minuend, essChannelId, callback);
		}

		// Subtrahends
		for (OpenemsComponent subtrahend : subtrahends) {
			if (subtrahend instanceof ElectricityMeter) {
				this.addOnSetNextValueListener(subtrahend, meterChannelId, callback);
			} else if (subtrahend instanceof SymmetricEss) {
				this.addOnSetNextValueListener(subtrahend, essChannelId, callback);
			}
		}
	}

	private void activateSubtractLong(OpenemsComponent minuend /* nullable */, List<OpenemsComponent> subtrahends,
			ElectricityMeter.ChannelId meterChannelId, SymmetricEss.ChannelId essChannelId) {
		final Consumer<Value<Integer>> callback = (ignore) -> {
			Long result = null;

			// Minuend
			if (minuend == null) {
				result = 0L;
			} else if (minuend instanceof ElectricityMeter meter) {
				LongReadChannel channel = meter.channel(meterChannelId);
				result = channel.getNextValue().get();
			} else if (minuend instanceof SymmetricEss) {
				LongReadChannel channel = ((SymmetricEss) minuend).channel(essChannelId);
				result = channel.getNextValue().get();
			}

			// Subtrahends
			for (OpenemsComponent subtrahend : subtrahends) {
				if (subtrahend instanceof ElectricityMeter meter) {
					LongReadChannel channel = meter.channel(meterChannelId);
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
		if (minuend == null) {
			// no listener for minuend
		} else if (minuend instanceof ElectricityMeter) {
			this.addOnSetNextValueListener(minuend, meterChannelId, callback);
		} else if (minuend instanceof SymmetricEss) {
			this.addOnSetNextValueListener(minuend, essChannelId, callback);
		}

		// Subtrahends
		for (OpenemsComponent subtrahend : subtrahends) {
			if (subtrahend instanceof ElectricityMeter) {
				this.addOnSetNextValueListener(subtrahend, meterChannelId, callback);
			} else if (subtrahend instanceof SymmetricEss) {
				this.addOnSetNextValueListener(subtrahend, essChannelId, callback);
			}
		}
	}

}
