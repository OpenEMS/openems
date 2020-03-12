package io.openems.edge.goodwe.et.charger;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.goodwe.et.ess.GoodWeEtBatteryInverter;

@Designate(ocd = ConfigPV1.class, factory = true)
@Component( //
		name = "GoodWe.ET.Charger-PV1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GoodWeEtChargerPv1 extends AbstractGoodWeEtCharger implements EssDcCharger, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private GoodWeEtBatteryInverter ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public GoodWeEtChargerPv1() {
		super();
	}

	/*
	 * Energy values since we don't have individual energy values. //
	 * 
	 * TODO update required from GoodWe regarding individual energy registers.
	 */

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		ModbusProtocol protocol = super.defineModbusProtocol();
		protocol.addTask(new FC3ReadRegistersTask(35191, Priority.LOW, //
				m(EssDcCharger.ChannelId.ACTUAL_ENERGY, new UnsignedDoublewordElement(35191),
						ElementToChannelConverter.SCALE_FACTOR_2)));
		return protocol;
	}

	@Activate
	void activate(ComponentContext context, ConfigPV1 config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		if (this.ess != null) {
			this.ess.addCharger(this);
		}
	}

	@Deactivate
	protected void deactivate() {
		if (this.ess != null) {
			this.ess.removeCharger(this);
		}
		super.deactivate();
	}

	@Override
	protected int getStartAddress() {
		return 35103;
	}

}
