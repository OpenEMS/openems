package io.openems.edge.ess.api;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * This lambda friendly functional class creates a Power Constraint when a new
 * write-value is sent to the Channel (setNextWriteValue()). This new constraint
 * is directly validated and only added if the Power problem is still solvable
 * with the new constraint. Otherwise an error is logged.
 */
public class PowerConstraint implements Consumer<Channel<?>> {

	private static final Logger log = LoggerFactory.getLogger(PowerConstraint.class);

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
	public void accept(Channel<?> channel) {
		((IntegerWriteChannel) channel).onSetNextWrite(value -> {
			if (value != null) {
				try {
					((ManagedSymmetricEss) channel.getComponent()).addPowerConstraintAndValidate(
							"Channel [" + this.channelId + "]", this.phase, this.pwr, this.relationship, value);
				} catch (PowerException e) {
					log.error(
							"Unable to set power constraint from Channel [" + this.channelId + "]: " + e.getMessage());
				}
			}
		});
	}

}
