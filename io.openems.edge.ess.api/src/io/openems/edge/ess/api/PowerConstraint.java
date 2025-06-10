package io.openems.edge.ess.api;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * This lambda friendly functional class creates a Power Constraint when a new
 * write-value is sent to the Channel (setNextWriteValue()). This new constraint
 * is directly validated and only added if the Power problem is still solvable
 * with the new constraint. Otherwise an error is logged.
 */
public class PowerConstraint implements ThrowingBiConsumer<ManagedSymmetricEss, Integer, OpenemsNamedException> {

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
	public void accept(ManagedSymmetricEss ess, Integer value) throws OpenemsNamedException {
		apply(ess, "Channel [" + this.channelId + "]", this.phase, this.pwr, this.relationship, value);
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
			final var power = ess.getPower();
			var v = switch (relationship) {
			case EQUALS:
				yield power.fitValueIntoMinMaxPower(description, ess, phase, pwr, value);
			case GREATER_OR_EQUALS:
				yield power.fitValueToMaxPower(description, ess, phase, pwr, value);
			case LESS_OR_EQUALS:
				yield power.fitValueToMinPower(description, ess, phase, pwr, value);
			};

			// set power channel constraint; throws an exception on error
			ess.addPowerConstraintAndValidate(description, phase, pwr, relationship, v);
		}
	}

}
