package io.openems.edge.ess.api;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * This lambda friendly functional class creates a Power Constraint when a new
 * write-value is sent to the Channel (setNextWriteValue()). This new constraint
 * is directly validated and only added if the Power problem is still solvable
 * with the new constraint. Otherwise an error is logged.
 */
public class PowerConstraint implements Consumer<Channel<Integer>> {

	private final String channelId;
	private final Phase phase;
	private final Pwr pwr;
	private final Relationship relationship;

	public PowerConstraint(String channelId, Phase phase, Pwr pwr, Relationship relationship) {
		this.channelId = channelId;
		this.phase = phase;
		this.pwr = pwr;
		this.relationship = relationship;
	}

	@Override
	public void accept(Channel<Integer> channel) {
		((IntegerWriteChannel) channel).onSetNextWrite(value -> {
			var ess = (ManagedSymmetricEss) channel.getComponent();
			apply(ess, "Channel [" + this.channelId + "]", this.phase, this.pwr, this.relationship, value);
		});
	}

	/**
	 * Apply a {@link PowerConstraint} to a {@link ManagedSymmetricEss}, defined by
	 * its parameters.
	 * 
	 * <p>
	 * The implementation assures, that the value fits the existing constraints.
	 * 
	 * @param ess          the target {@link ManagedSymmetricEss}
	 * @param description  a descriptive text for log messages
	 * @param phase        the target {@link Phase}h
	 * @param pwr          the {@link Pwr} mode
	 * @param relationship the {@link Relationship}
	 * @param value        the power value in [W] or [var]
	 * @throws OpenemsException on error
	 */
	public static void apply(ManagedSymmetricEss ess, String description, Phase phase, Pwr pwr,
			Relationship relationship, Integer value) throws OpenemsException {
		if (value != null) {
			// adjust value so that it fits into Min/MaxActivePower
			switch (relationship) {
			case EQUALS:
				value = ess.getPower().fitValueIntoMinMaxPower(description, ess, phase, pwr, value);
				break;
			case GREATER_OR_EQUALS:
				value = ess.getPower().fitValueToMaxPower(description, ess, phase, pwr, value);
				break;
			case LESS_OR_EQUALS:
				value = ess.getPower().fitValueToMinPower(description, ess, phase, pwr, value);
				break;
			}

			// set power channel constraint; throws an exception on error
			ess.addPowerConstraintAndValidate(description, phase, pwr, relationship, value);
		}
	}

}
