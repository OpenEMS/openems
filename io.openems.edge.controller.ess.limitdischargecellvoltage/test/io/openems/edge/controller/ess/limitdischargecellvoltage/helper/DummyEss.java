package io.openems.edge.controller.ess.limitdischargecellvoltage.helper;

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
	public static int DEFAULT_SOC = 50;
	public static int DEFAULT_MIN_CELL_VOLTAGE = 3280;
	public static int DEFAULT_MAX_CELL_VOLTAGE = 3380;
	public static int DEFAULT_MIN_CELL_TEMPERATURE = 25;
	public static int DEFAULT_MAX_CELL_TEMPERATURE = 33;
	
	private int currentActivePower = 0;

	protected DummyEss(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		
		setMinimalCellVoltage(DEFAULT_MIN_CELL_VOLTAGE);
		setMaximalCellVoltage(DEFAULT_MAX_CELL_VOLTAGE);
		setMinimalCellTemperature(DEFAULT_MIN_CELL_TEMPERATURE);
		setMaximalCellTemperature(DEFAULT_MAX_CELL_TEMPERATURE);
		setSoc(DEFAULT_SOC);
		setCurrentActivePower(0);
	}

	public void setMinimalCellVoltage(int minimalCellVoltage) {
		this.getMinCellVoltage().setNextValue(minimalCellVoltage);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMinimalCellVoltageToUndefined() {
		this.getMinCellVoltage().setNextValue(null);
		this.getMinCellVoltage().nextProcessImage();
	}

	public void setMaximalCellVoltage(int maximalCellVoltage) {
		this.getMaxCellVoltage().setNextValue(maximalCellVoltage);
		this.getMaxCellVoltage().nextProcessImage();
	}

	public void setMaximalCellVoltageToUndefined() {
		this.getMaxCellVoltage().setNextValue(null);
		this.getMaxCellVoltage().nextProcessImage();
	}
	
	public void setMinimalCellTemperature(int minimalCellTemperature) {
		this.getMinCellTemperature().setNextValue(minimalCellTemperature);
		this.getMinCellTemperature().nextProcessImage();
	}

	public void setMinimalCellTemperatureToUndefined() {
		this.getMinCellTemperature().setNextValue(null);
		this.getMinCellTemperature().nextProcessImage();
	}

	public void setMaximalCellTemperature(int maximalCellTemperature) {
		this.getMaxCellTemperature().setNextValue(maximalCellTemperature);
		this.getMaxCellTemperature().nextProcessImage();
	}

	public void setMaximalCellTemperatureToUndefined() {
		this.getMaxCellTemperature().setNextValue(null);
		this.getMaxCellTemperature().nextProcessImage();
	}
	
	public void setSoc(int soc) {
		this.getSoc().setNextValue(soc);
		this.getSoc().nextProcessImage();
	}

	public void setSocToUndefined() {
		this.getSoc().setNextValue(null);
		this.getSoc().nextProcessImage();
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
			public PidFilter buildPidFilter() {
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
