package io.openems.edge.controller.ess.limitdischargecellvoltage.helper;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.common.component.AbstractOpenemsComponent;

public class DummyEss extends AbstractOpenemsComponent implements ManagedSymmetricEss {

	public static int MAXIMUM_POWER = 10000;

	protected DummyEss(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this.getMinCellVoltage().setNextValue(minimalCellVoltage);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMinimalCellVoltageToUndefined() {
		this.getMinCellVoltage().setNextValue(null);
		this.getMinCellVoltage().nextProcessImage();
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
				return constraint;
			}

			@Override
			public Constraint addConstraint(Constraint constraint) {
				return constraint;
			}
		};
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}
}
