package io.openems.edge.controller.battery.batteryprotection.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class DummyEss extends AbstractOpenemsComponent implements ManagedSymmetricEss {

	public static int MAXIMUM_POWER = 10000;
	private int currentActivePower = 0;

	protected DummyEss(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) { //
		super(firstInitialChannelIds, furtherInitialChannelIds);

		setCurrentActivePower(0);
	}

	public int getCurrentActivePower() {
		return currentActivePower;
	}

	public void setCurrentActivePower(int power) {
		currentActivePower = power;
		this.getActivePower().setNextValue(power);
		this.getActivePower().nextProcessImage();
	}

	@Override
	public Power getPower() {

		return new Power() {

			@Override
			public void removeConstraint(Constraint constraint) {
			}

			@Override
			public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
				return (-1) * MAXIMUM_POWER;
			}

			@Override
			public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
				return MAXIMUM_POWER;
			}

			@Override
			public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) throws OpenemsException {
				return null;
			}

			@Override
			public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
					Relationship relationship, double value) throws OpenemsException {
				Coefficient coefficient = new Coefficient(0, ess.id(), phase, pwr);
				LinearCoefficient lc = new LinearCoefficient(coefficient, value);
				LinearCoefficient[] coefficients = { lc };
				return new Constraint(description, coefficients, relationship, value);
			}

			@Override
			public Constraint addConstraintAndValidate(Constraint constraint) throws OpenemsException {
				return addConstraint(constraint);
			}

			@Override
			public Constraint addConstraint(Constraint constraint) {
				switch (constraint.getRelationship()) {
				case EQUALS:
					currentActivePower = constraint.getValue().get().intValue();
					break;
				case GREATER_OR_EQUALS:
					currentActivePower = Math.max(currentActivePower, constraint.getValue().get().intValue());
					break;
				case LESS_OR_EQUALS:
					currentActivePower = Math.min(currentActivePower, constraint.getValue().get().intValue());
					break;
				default:
					break;

				}
				return constraint;
			}

			@Override
			public PidFilter getPidFilter() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.currentActivePower = activePower;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}
}
