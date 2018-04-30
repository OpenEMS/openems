package io.openems.edge.ess.fenecon.commercial40.charger;

import java.util.concurrent.atomic.AtomicReference;

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

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.converter.ChannelConverterSumInteger;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.fenecon.commercial40.EssFeneconCommercial40;
import io.openems.edge.ess.symmetric.api.EssSymmetric;

/**
 * Implements the FENECON Commercial 40 Charger
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "EssDcCharger.Fenecon.Commercial40", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EssDcChargerFeneconCommercial40 extends AbstractOpenemsModbusComponent
		implements EssDcCharger, OpenemsComponent {

	// private final Logger log =
	// LoggerFactory.getLogger(EssDcChargerFeneconCommercial40.class);

	private AtomicReference<EssFeneconCommercial40> ess = new AtomicReference<EssFeneconCommercial40>(null);

	@Reference
	protected ConfigurationAdmin cm;

	public EssDcChargerFeneconCommercial40() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setEss(EssSymmetric ess) {
		if (ess instanceof EssFeneconCommercial40) {
			this.ess.set((EssFeneconCommercial40) ess);
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), this.ess.get().getUnitId(),
				this.cm, "Modbus", this.ess.get().getModbusBridgeId());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		PV_DCDC0_INPUT_POWER(new Doc().unit(Unit.WATT)), //
		PV_DCDC1_INPUT_POWER(new Doc().unit(Unit.WATT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		ModbusProtocol protocol = new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(0xA735, Priority.HIGH, //
						m(ChannelId.PV_DCDC0_INPUT_POWER, new SignedWordElement(0xA735),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC3ReadRegistersTask(0xA735, Priority.HIGH, //
						m(ChannelId.PV_DCDC1_INPUT_POWER, new SignedWordElement(0xA735),
								ElementToChannelConverter.SCALE_FACTOR_2)));
		/*
		 * Merge PV_DCDC0_INPUT_POWER and PV_DCDC1_INPUT_POWER to ACTUAL_POWER
		 */
		new ChannelConverterSumInteger( //
				/* target */ this.getActualPower(), //
				/* sources */ (Channel<Integer>[]) new Channel<?>[] { //
						this.<Channel<Integer>>channel(ChannelId.PV_DCDC0_INPUT_POWER), //
						this.<Channel<Integer>>channel(ChannelId.PV_DCDC1_INPUT_POWER) //
				});

		// Runnable mergeActualPower = () -> {
		// int PV_DCDC0_INPUT_POWER = latestPV_DCDC0_INPUT_POWER.get();
		// int PV_DCDC1_INPUT_POWER = latestPV_DCDC1_INPUT_POWER.get();
		// try {
		// this.getActualPower().setNextValue(PV_DCDC0_INPUT_POWER +
		// PV_DCDC1_INPUT_POWER);
		// } catch (OpenemsException e) {
		// logError(log, "Unable to merge ACTUAL_POWER from [" + PV_DCDC0_INPUT_POWER +
		// "] and ["
		// + PV_DCDC1_INPUT_POWER + "]: " + e.getMessage());
		// }
		// };
		// this.<Channel<Integer>>channel(ChannelId.PV_DCDC0_INPUT_POWER).onUpdate(value
		// -> {
		// if (value != null) {
		// latestPV_DCDC0_INPUT_POWER.set(value);
		// } else {
		// latestPV_DCDC0_INPUT_POWER.set(0);
		// }
		// mergeActualPower.run();
		// });
		// this.<Channel<Integer>>channel(ChannelId.PV_DCDC1_INPUT_POWER).onUpdate(value
		// -> {
		// if (value != null) {
		// latestPV_DCDC1_INPUT_POWER.set(value);
		// } else {
		// latestPV_DCDC1_INPUT_POWER.set(0);
		// }
		// mergeActualPower.run();
		// });
		return protocol;
	}

	@Override
	public String debugLog() {
		return "P:" + this.getActualPower().format();
	}
}
