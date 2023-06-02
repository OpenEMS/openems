package io.openems.edge.goodwe.charger;

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
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = ConfigPV1.class, factory = true)
@Component(//
		name = "GoodWe.Charger-PV1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GoodWeEtCharger1 extends AbstractGoodWeEtCharger
		implements GoodWeEtCharger, EssDcCharger, ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private GoodWe essOrBatteryInverter;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public GoodWeEtCharger1() {
	}

	@Activate
	private void activate(ComponentContext context, ConfigPV1 config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "essOrBatteryInverter",
				config.essOrBatteryInverter_id())) {
			return;
		}

		if (this.essOrBatteryInverter != null) {
			this.essOrBatteryInverter.addCharger(this);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.essOrBatteryInverter != null) {
			this.essOrBatteryInverter.removeCharger(this);
		}
		super.deactivate();
	}

	@Override
	protected int getStartAddress() {
		return 35103;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected GoodWe getEssOrBatteryInverter() {
		return this.essOrBatteryInverter;
	}
}
