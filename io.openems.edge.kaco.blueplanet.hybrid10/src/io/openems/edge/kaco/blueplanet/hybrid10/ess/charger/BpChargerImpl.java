package io.openems.edge.kaco.blueplanet.hybrid10.ess.charger;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.ed.data.InverterData;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.kaco.blueplanet.hybrid10.core.BpCore;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kaco.BlueplanetHybrid10.Charger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class BpChargerImpl extends AbstractOpenemsComponent
		implements BpCharger, EssDcCharger, OpenemsComponent, EventHandler {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BpCore core;

	public BpChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				BpCharger.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().value().asString();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}

	}

	private void updateChannels() {
		Integer actualPower = null;
		InverterData inverter = this.core.getInverterData();
		if (inverter != null) {
			actualPower = Math.round(inverter.getPvPower());
		}
		this.getActualPower().setNextValue(actualPower);
	}
}
