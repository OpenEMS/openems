package io.openems.edge.ess.api;

import java.util.function.Consumer;

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
			if (value != null) {
				ManagedSymmetricEss ess = (ManagedSymmetricEss) channel.getComponent();
				
				// adjust value so that it fits into Min/MaxActivePower
				value = ess.getPower().fitValueIntoMinMaxPower(ess, this.phase, this.pwr, value);

				// set power channel constraint; throws an exception on error
				ess.addPowerConstraintAndValidate("Channel [" + this.channelId + "]", this.phase, this.pwr,
						this.relationship, value);
			}
		});
	}

}
