package io.openems.edge.fenecon.dess.charger;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.fenecon.dess.ess.FeneconDessEss;

@Designate(ocd = Config2.class, factory = true)
@Component(//
		name = "Fenecon.Dess.Charger2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FeneconDessCharger2 extends AbstractFeneconDessCharger
		implements FeneconDessCharger, EssDcCharger, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private FeneconDessEss ess;

	public FeneconDessCharger2() {
		super();
	}

	@Activate
	void activate(ComponentContext context, Config1 config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.ess.getUnitId(), this.cm,
				"Modbus", this.ess.getModbusBridgeId())) {
			return;
		}

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		this.ess.addCharger(this);
	}

	@Deactivate
	protected void deactivate() {
		this.ess.removeCharger(this);
		super.deactivate();
	}

	@Override
	protected int getOffset() {
		return 11230;
	}

}