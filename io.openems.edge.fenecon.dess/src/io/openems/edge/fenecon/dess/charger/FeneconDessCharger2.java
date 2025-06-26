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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.fenecon.dess.ess.FeneconDessEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config2.class, factory = true)
@Component(//
		name = "Fenecon.Dess.Charger2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class FeneconDessCharger2 extends AbstractFeneconDessCharger
		implements FeneconDessCharger, EssDcCharger, ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private FeneconDessEss ess;

	public FeneconDessCharger2() {
	}

	@Activate
	private void activate(ComponentContext context, Config2 config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.ess.getUnitId(), this.cm,
				"Modbus", this.ess.getModbusBridgeId())) {
			return;
		}

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		this.ess.addCharger(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.ess.removeCharger(this);
		super.deactivate();
	}

	@Override
	protected int getOffset() {
		return 11230;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
