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
import io.openems.edge.common.sum.SumOptions;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

@Component(service = { ChargerHandlerImpl.class }) //
public class ChargerHandlerImpl {

	private final List<EssDcCharger> chargers = new CopyOnWriteArrayList<>();
	private final CalculateIntegerSum productionDcActualPower = new CalculateIntegerSum();
	private final CalculateLongSum productionDcActiveEnergy = new CalculateLongSum();

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			policyOption = ReferencePolicyOption.GREEDY, //
			target = "(enabled=true)"//
	)
	protected void addCharger(EssDcCharger component) {
		this.chargers.add(component);
	}

	protected void removeCharger(EssDcCharger component) {
		this.chargers.remove(component);
	}

	@Activate
	public ChargerHandlerImpl() {
	}

	/**
	 * Calculates the sum-values for all registered DC chargers.
	 * 
	 * <p>
	 * This method resets the internal calculators and iterates through all enabled
	 * chargers to aggregate their power and energy values.
	 */
	public void calculate() {
		this.productionDcActualPower.reset();
		this.productionDcActiveEnergy.reset();
		for (var charger : this.chargers) {
			if (charger instanceof SumOptions sumOption && !sumOption.addToSum()) {
				continue;
			}
			this.productionDcActualPower.addValue(charger.getActualPowerChannel());
			this.productionDcActiveEnergy.addValue(charger.getActualEnergyChannel());
		}
	}

	public Integer getProductionDcActualPower() {
		return this.productionDcActualPower.calculate();
	}

	public Long getProductionDcActiveEnergy() {
		return this.productionDcActiveEnergy.calculate();
	}

}