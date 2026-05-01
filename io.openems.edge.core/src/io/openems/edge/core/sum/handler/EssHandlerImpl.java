package io.openems.edge.core.sum.handler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.SumOptions;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.CalculateGridMode;
import io.openems.edge.ess.api.CalculateSoc;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.SymmetricEss;

@Component(service = { EssHandlerImpl.class })
public class EssHandlerImpl {

	private final List<SymmetricEss> esss = new CopyOnWriteArrayList<>();
	private final CalculateSoc essSoc = new CalculateSoc();
	private final CalculateIntegerSum essActivePower = new CalculateIntegerSum();
	private final CalculateIntegerSum essActivePowerL1 = new CalculateIntegerSum();
	private final CalculateIntegerSum essActivePowerL2 = new CalculateIntegerSum();
	private final CalculateIntegerSum essActivePowerL3 = new CalculateIntegerSum();
	private final CalculateIntegerSum essReactivePower = new CalculateIntegerSum();
	private final CalculateIntegerSum essMaxApparentPower = new CalculateIntegerSum();
	private final CalculateGridMode essGridMode = new CalculateGridMode();
	private final CalculateLongSum essActiveChargeEnergy = new CalculateLongSum();
	private final CalculateLongSum essActiveDischargeEnergy = new CalculateLongSum();
	private final CalculateLongSum essDcChargeEnergy = new CalculateLongSum();
	private final CalculateLongSum essDcDischargeEnergy = new CalculateLongSum();
	private final CalculateIntegerSum essCapacity = new CalculateIntegerSum();
	private final CalculateIntegerSum essDcDischargePower = new CalculateIntegerSum();

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			policyOption = ReferencePolicyOption.GREEDY, //
			target = "(enabled=true)"//
	)
	protected void addEss(SymmetricEss component) {
		this.esss.add(component);
	}

	protected void removeEss(SymmetricEss component) {
		this.esss.remove(component);
	}

	@Activate
	public EssHandlerImpl() {
	}

	/**
	 * Calculates the sum-values for all registered ESS components.
	 * 
	 * <p>
	 * This method resets all internal calculators and aggregates values from
	 * Symmetric, Asymmetric, and Hybrid ESS components.
	 */
	public void calculate() {
		this.resetCalculators();

		for (var ess : this.esss) {
			if (ess instanceof SumOptions sumOption && !sumOption.addToSum()) {
				continue;
			}
			this.essSoc.add(ess);
			this.essActivePower.addValue(ess.getActivePowerChannel());
			this.essReactivePower.addValue(ess.getReactivePowerChannel());
			this.essMaxApparentPower.addValue(ess.getMaxApparentPowerChannel());
			this.essGridMode.addValue(ess.getGridModeChannel());
			this.essActiveChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
			this.essActiveDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
			this.essCapacity.addValue(ess.getCapacityChannel());

			if (ess instanceof AsymmetricEss e) {
				this.essActivePowerL1.addValue(e.getActivePowerL1Channel());
				this.essActivePowerL2.addValue(e.getActivePowerL2Channel());
				this.essActivePowerL3.addValue(e.getActivePowerL3Channel());
			} else {
				this.essActivePowerL1.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
				this.essActivePowerL2.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
				this.essActivePowerL3.addValue(ess.getActivePowerChannel(), CalculateIntegerSum.DIVIDE_BY_THREE);
			}

			if (ess instanceof HybridEss e) {
				this.essDcChargeEnergy.addValue(e.getDcChargeEnergyChannel());
				this.essDcDischargeEnergy.addValue(e.getDcDischargeEnergyChannel());
				this.essDcDischargePower.addValue(e.getDcDischargePowerChannel());
			} else {
				this.essDcChargeEnergy.addValue(ess.getActiveChargeEnergyChannel());
				this.essDcDischargeEnergy.addValue(ess.getActiveDischargeEnergyChannel());
				this.essDcDischargePower.addValue(ess.getActivePowerChannel());
			}
		}
	}

	private void resetCalculators() {
		this.essSoc.reset();
		this.essActivePower.reset();
		this.essActivePowerL1.reset();
		this.essActivePowerL2.reset();
		this.essActivePowerL3.reset();
		this.essReactivePower.reset();
		this.essMaxApparentPower.reset();
		this.essGridMode.reset();
		this.essActiveChargeEnergy.reset();
		this.essActiveDischargeEnergy.reset();
		this.essDcChargeEnergy.reset();
		this.essDcDischargeEnergy.reset();
		this.essCapacity.reset();
		this.essDcDischargePower.reset();
	}

	public Integer getSoc() {
		return this.essSoc.calculate();
	}

	public Integer getActivePower() {
		return this.essActivePower.calculate();
	}

	public Integer getActivePowerL1() {
		return this.essActivePowerL1.calculate();
	}

	public Integer getActivePowerL2() {
		return this.essActivePowerL2.calculate();
	}

	public Integer getActivePowerL3() {
		return this.essActivePowerL3.calculate();
	}

	public Integer getReactivePower() {
		return this.essReactivePower.calculate();
	}

	public Integer getMaxApparentPower() {
		return this.essMaxApparentPower.calculate();
	}

	public GridMode getGridMode() {
		return this.essGridMode.calculate();
	}

	public Long getActiveChargeEnergy() {
		return this.essActiveChargeEnergy.calculate();
	}

	public Long getActiveDischargeEnergy() {
		return this.essActiveDischargeEnergy.calculate();
	}

	public Long getDcChargeEnergy() {
		return this.essDcChargeEnergy.calculate();
	}

	public Long getDcDischargeEnergy() {
		return this.essDcDischargeEnergy.calculate();
	}

	public Integer getCapacity() {
		return this.essCapacity.calculate();
	}

	public Integer getDcDischargePower() {
		return this.essDcDischargePower.calculate();
	}

}