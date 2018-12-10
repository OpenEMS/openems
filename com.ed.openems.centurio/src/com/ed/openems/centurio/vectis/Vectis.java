package com.ed.openems.centurio.vectis;

import java.net.UnknownHostException;

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

import com.ed.data.VectisData;
import com.ed.openems.centurio.CenturioConstants;
import com.ed.openems.centurio.datasource.api.EdComData;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "KACO.hy-switch", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class Vectis extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected EdComData datasource;

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Datasource", config.datasource_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public Vectis() {
		VectisUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
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
		Integer reactivePower = null;
		Integer reactivePowerL1 = null;
		Integer reactivePowerL2 = null;
		Integer reactivePowerL3 = null;
		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		

		if (this.datasource.isConnected()) {
			VectisData vectis = this.datasource.getVectis();

			reactivePowerL1 = CenturioConstants.roundToPowerPrecision(vectis.getReactivePower(0));
			reactivePowerL2 = CenturioConstants.roundToPowerPrecision(vectis.getReactivePower(1));
			reactivePowerL3 = CenturioConstants.roundToPowerPrecision(vectis.getReactivePower(2));
			reactivePower = reactivePowerL1 + reactivePowerL2 + reactivePowerL3;

			activePowerL1 = CenturioConstants.roundToPowerPrecision(vectis.getACPower(0));
			activePowerL2 = CenturioConstants.roundToPowerPrecision(vectis.getACPower(1));
			activePowerL3 = CenturioConstants.roundToPowerPrecision(vectis.getACPower(2));
			activePower = activePowerL1 + activePowerL2 + activePowerL3;
		}

		this.getReactivePowerL1().setNextValue(reactivePowerL1);
		this.getReactivePowerL2().setNextValue(reactivePowerL2);
		this.getReactivePowerL3().setNextValue(reactivePowerL3);
		this.getReactivePower().setNextValue(reactivePower);
		this.getActivePowerL1().setNextValue(activePowerL1);
		this.getActivePowerL2().setNextValue(activePowerL2);
		this.getActivePowerL3().setNextValue(activePowerL3);
		this.getActivePower().setNextValue(activePower);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}
}
