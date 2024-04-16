package io.openems.edge.deye.common.charger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.deye.common.DeyeSunHybrid;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
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

@Designate(ocd = ConfigPv2.class, factory = true)
@Component(//
		name = "Ess.Fenecon.Commercial40.PV2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class DeyeSun2Impl extends AbstractEssDeyePv
		implements DeyeSunPv, EssDcCharger, ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private DeyeSunHybrid ess;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public DeyeSun2Impl() {
	}

	@Activate
	private void activate(ComponentContext context, ConfigPv2 config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.ess.getUnitId(), this.cm,
				"Modbus", this.ess.getModbusBridgeId())) {
			return;
		}

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Ess", config.ess_id())) {
			return;
		}

		this.ess.addCharger(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.ess != null) {
			this.ess.removeCharger(this);
		}
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected boolean isPV1() {
		return false;
	}
}
