package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

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

import com.ed.data.InverterData;
import com.ed.data.Status;
import com.ed.data.VectisData;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.kaco.blueplanet.hybrid10.GlobalUtils;
import io.openems.edge.kaco.blueplanet.hybrid10.core.BpCore;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Kaco.BlueplanetHybrid10.GridMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class VectisImpl extends AbstractOpenemsComponent
		implements Vectis, SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BpCore core;

	@Reference
	protected ConfigurationAdmin cm;

	private Config config;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public VectisImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				Vectis.ChannelId.values() //
		);
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
		Integer freq = null;
		Integer vectisStatus = null;

		if (this.core.isConnected()) {
			VectisData vectis = this.core.getVectis();
			InverterData inverter = this.core.getInverterData();

			if (this.config.external()) {
				// Use external sensor
				reactivePowerL1 = GlobalUtils.roundToPowerPrecision(vectis.getReactivePowerExt(0));
				reactivePowerL2 = GlobalUtils.roundToPowerPrecision(vectis.getReactivePowerExt(1));
				reactivePowerL3 = GlobalUtils.roundToPowerPrecision(vectis.getReactivePowerExt(2));
				reactivePower = reactivePowerL1 + reactivePowerL2 + reactivePowerL3;

				activePowerL1 = GlobalUtils.roundToPowerPrecision(vectis.getACPowerExt(0));
				activePowerL2 = GlobalUtils.roundToPowerPrecision(vectis.getACPowerExt(1));
				activePowerL3 = GlobalUtils.roundToPowerPrecision(vectis.getACPowerExt(2));
				activePower = activePowerL1 + activePowerL2 + activePowerL3;

				freq = Math.round(vectis.getFrequencyExt());

			} else {
				// Use internal sensor
				reactivePowerL1 = GlobalUtils.roundToPowerPrecision(vectis.getReactivePower(0));
				reactivePowerL2 = GlobalUtils.roundToPowerPrecision(vectis.getReactivePower(1));
				reactivePowerL3 = GlobalUtils.roundToPowerPrecision(vectis.getReactivePower(2));
				reactivePower = reactivePowerL1 + reactivePowerL2 + reactivePowerL3;

				activePowerL1 = GlobalUtils.roundToPowerPrecision(vectis.getACPower(0));
				activePowerL2 = GlobalUtils.roundToPowerPrecision(vectis.getACPower(1));
				activePowerL3 = GlobalUtils.roundToPowerPrecision(vectis.getACPower(2));
				activePower = activePowerL1 + activePowerL2 + activePowerL3;

				freq = Math.round(inverter.getGridFrequency());
			}

			Status status = this.core.getStatusData();
			if (status != null) {
				vectisStatus = status.getVectisStatus();
			}

		}

		this._setReactivePowerL1(reactivePowerL1);
		this._setReactivePowerL2(reactivePowerL2);
		this._setReactivePowerL3(reactivePowerL3);
		this._setReactivePower(reactivePower);
		this._setActivePowerL1(activePowerL1);
		this._setActivePowerL2(activePowerL2);
		this._setActivePowerL3(activePowerL3);
		this._setActivePower(activePower);
		this._setFrequency(freq);
		this.channel(Vectis.ChannelId.VECTIS_STATUS).setNextValue(vectisStatus);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}
}
