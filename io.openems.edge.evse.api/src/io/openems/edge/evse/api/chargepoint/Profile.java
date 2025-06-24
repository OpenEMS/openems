package io.openems.edge.evse.api.chargepoint;

import io.openems.edge.evse.api.Limit;

public interface Profile {

	/**
	 * Declares the Abilities of an {@link EvseChargePoint}.
	 */
	public static record ChargePointAbilities(ApplySetPoint.Ability applySetPoint, PhaseSwitch.Ability phaseSwitch) {

		public static final class Builder {

			private ApplySetPoint.Ability applySetPoint = null;
			private PhaseSwitch.Ability phaseSwitch = null;

			/**
			 * Defines the {@link Profile.ApplySetPoint.Ability}.
			 * 
			 * @param ability the ability
			 * @return the {@link Builder}
			 */
			public Builder applySetPointIn(ApplySetPoint.Ability ability) {
				this.applySetPoint = ability;
				return this;
			}

			/**
			 * Defines the {@link Profile.PhaseSwitch.Ability}.
			 * 
			 * @param ability the ability
			 * @return the {@link Builder}
			 */
			public Builder phaseSwitch(PhaseSwitch.Ability ability) {
				this.phaseSwitch = ability;
				return this;
			}

			public ChargePointAbilities build() {
				return new ChargePointAbilities(this.applySetPoint, this.phaseSwitch);
			}
		}

		/**
		 * Create a {@link ChargePointAbilities} builder.
		 *
		 * @return a {@link Builder}
		 */
		public static Builder create() {
			return new Builder();
		}
	}

	/**
	 * Declares the Actions for an {@link EvseChargePoint}.
	 */
	public static record ChargePointActions(ApplySetPoint.Action applySetPoint, PhaseSwitch.Action phaseSwitch) {

		/**
		 * Gets the {@link ApplySetPoint} in [A].
		 * 
		 * @return the object
		 */
		public ApplySetPoint.Action.Ampere getApplySetPointInAmpere() {
			if (this.applySetPoint instanceof ApplySetPoint.Action.Ampere a) {
				return a;
			}
			throw new IllegalArgumentException(
					"ApplySetPoint is of invalid type [" + this.applySetPoint.getClass().getSimpleName() + "]");
		}

		/**
		 * Gets the {@link ApplySetPoint} in [mA].
		 * 
		 * @return the object
		 */
		public ApplySetPoint.Action.MilliAmpere getApplySetPointInMilliAmpere() {
			if (this.applySetPoint instanceof ApplySetPoint.Action.MilliAmpere ma) {
				return ma;
			}
			throw new IllegalArgumentException(
					"ApplySetPoint is of invalid type [" + this.applySetPoint.getClass().getSimpleName() + "]");
		}

		public static final class Builder {

			private final ChargePointAbilities abilities;
			private ApplySetPoint.Action applySetPoint = null;
			private PhaseSwitch.Action phaseSwitch = null;

			private Builder(ChargePointAbilities abilities) {
				this.abilities = abilities;
			}

			public Builder setApplySetPointInMilliAmpere(int value) throws IllegalArgumentException {
				return this.setApplySetPoint(new Profile.ApplySetPoint.Action.MilliAmpere(value));
			}

			public Builder setApplySetPointInAmpere(int value) throws IllegalArgumentException {
				return this.setApplySetPoint(new Profile.ApplySetPoint.Action.Ampere(value));
			}

			public Builder setApplySetPoint(ApplySetPoint.Action applySetPoint) throws IllegalArgumentException {
				this.applySetPoint = switch (applySetPoint) {
				case ApplySetPoint.Action.MilliAmpere ma when this.abilities.applySetPoint == ApplySetPoint.Ability.MILLI_AMPERE //
					-> ma;
				case ApplySetPoint.Action.Ampere a when this.abilities.applySetPoint == ApplySetPoint.Ability.AMPERE //
					-> a;
				case null, default -> throw new IllegalArgumentException(
						"ApplySetPoint action must be of type [" + this.abilities.applySetPoint + "]");
				};
				return this;
			}

			public Builder setPhaseSwitch(PhaseSwitch.Action phaseSwitch) {
				this.phaseSwitch = switch (this.abilities.phaseSwitch) {
				case PhaseSwitch.Ability.ToSinglePhase tsp when phaseSwitch == PhaseSwitch.Action.TO_SINGLE_PHASE //
					-> phaseSwitch;
				case PhaseSwitch.Ability.ToThreePhase ttp when phaseSwitch == PhaseSwitch.Action.TO_THREE_PHASE //
					-> phaseSwitch;
				case null, default -> throw new IllegalArgumentException(
						"PhaseSwitch action must be [" + this.abilities.phaseSwitch.getClass().getSimpleName() + "]");
				};
				return this;
			}

			public ChargePointActions build() throws IllegalArgumentException {
				if (this.applySetPoint == null) {
					throw new IllegalArgumentException("ApplySetPoint is always required");
				}
				return new ChargePointActions(this.applySetPoint, this.phaseSwitch);
			}
		}

		/**
		 * Factory.
		 *
		 * @param abilities the {@link ChargePointAbilities}
		 * @return a {@link Builder}
		 */
		public static Builder from(ChargePointAbilities abilities) {
			return new Builder(abilities);
		}
	}

	/**
	 * Different types of applying a set-point.
	 */
	public static final class ApplySetPoint {

		private ApplySetPoint() {
		}

		public static enum Ability {
			MILLI_AMPERE, AMPERE;
		}

		public sealed interface Action {

			/**
			 * Gets the value.
			 * 
			 * @return the value in the unit defined by its class
			 */
			public int value();

			public static record MilliAmpere(int value) implements ApplySetPoint.Action {
			}

			public static record Ampere(int value) implements ApplySetPoint.Action {
			}
		}
	}

	/**
	 * Different types of applying a set-point.
	 */
	public static final class PhaseSwitch {

		private PhaseSwitch() {
		}

		public sealed interface Ability {

			public static record ToSinglePhase(Limit singlePhaseLimit) implements PhaseSwitch.Ability {
			}

			public static record ToThreePhase(Limit threePhaseLimit) implements PhaseSwitch.Ability {
			}
		}

		public static enum Action {
			TO_SINGLE_PHASE, TO_THREE_PHASE;
		}

	}
}
