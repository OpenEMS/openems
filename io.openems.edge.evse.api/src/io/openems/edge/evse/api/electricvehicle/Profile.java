package io.openems.edge.evse.api.electricvehicle;

public sealed interface Profile {

	/**
	 * EV does not support interrupting a charging session. Instead charge current
	 * is reduced to minimum by the Controller in this case.
	 */
	public static final NoInterrupt NO_INTERRUPT = new NoInterrupt();

	// TODO use in SmartMode
	// var canInterrupt =
	// electricVehicle.profiles().stream().noneMatch(Profile.NoInterrupt.class::isInstance);
	public record NoInterrupt() implements Profile {
	}

}
