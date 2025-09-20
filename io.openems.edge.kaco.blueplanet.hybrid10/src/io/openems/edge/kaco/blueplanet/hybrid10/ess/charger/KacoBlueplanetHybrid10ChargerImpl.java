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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.kaco.blueplanet.hybrid10.core.KacoBlueplanetHybrid10Core;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kaco.BlueplanetHybrid10.Charger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class KacoBlueplanetHybrid10ChargerImpl extends AbstractOpenemsComponent implements
		KacoBlueplanetHybrid10Charger, EssDcCharger, OpenemsComponent, TimedataProvider, EventHandler, ModbusSlave {

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private KacoBlueplanetHybrid10Core core;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public KacoBlueplanetHybrid10ChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				KacoBlueplanetHybrid10Charger.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().asString();
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

		var bpData = this.core.getBpData();
		this._setCommunicationFailed(bpData == null);

		if (bpData != null) {
			actualPower = Math.round(bpData.inverter.getPvPower());
		}
		this._setActualPower(actualPower);

		// Calculate Energy
		this.calculateActualEnergy.update(actualPower);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(KacoBlueplanetHybrid10Charger.class, accessMode, 100) //
						.build());
	}
}
