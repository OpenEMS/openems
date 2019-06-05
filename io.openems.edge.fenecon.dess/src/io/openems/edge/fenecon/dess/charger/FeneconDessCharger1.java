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

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.fenecon.dess.FeneconDessConstants;

@Designate(ocd = Config1.class, factory = true)
@Component( //
		name = "Fenecon.Dess.Charger1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class FeneconDessCharger1 extends AbstractFeneconDessCharger implements EssDcCharger, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public FeneconDessCharger1() {
		super();
	}

	@Activate
	void activate(ComponentContext context, Config1 config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), FeneconDessConstants.UNIT_ID, this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected int getOffset() {
		return 11240;
	}

}