package io.openems.edge.controller.symmetric.reactivepowervoltagecharacteristic;

import java.util.HashMap;
import java.util.Map;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.ReactivePowerVoltageCharacteristic", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ReactivePowerVoltageCharacteristic extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent {

	private float nominalVoltage;
	private Map<Float, Float> qCharacteristic = new HashMap<>();
	LongReadChannel maxNominalPower;

	@Reference
	protected ConfigurationAdmin cm;

	private int power = 0;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}
		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "meter", config.meter_id())) {
			return;
		}

		this.nominalVoltage = config.nominalVoltage();
		initialize(config);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private ManagedSymmetricEss ess;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/*
	 * Methods
	 */
	private void initialize(Config config) throws OpenemsException {
		// Parsing JSon then putting the point variables into qByUCharacteristicEquation
		// [{ "voltage" : 0.9,"percent" : 60 }, { "voltage":0.93,"percent": 0},
		// {"voltage":
		// 1.07 ,"percent": 0 },{"voltage": 1.1 ,"percent": -60 }]
		// ...
		try {
			JsonElement jElement = JsonUtils.parse(config.percentQ());
			JsonArray jArray = JsonUtils.getAsJsonArray(jElement);
			JsonElement jEl;
			float percent = 0, voltage = 0;
			for (int i = 0; i < jArray.size(); i++) {
				jEl = jArray.get(i);
				percent = jEl.getAsJsonObject().get("percent").getAsFloat();
				voltage = jEl.getAsJsonObject().get("voltage").getAsFloat();
				qCharacteristic.put(voltage, percent);
			}
		} catch (NullPointerException | OpenemsNamedException e) {
			throw new OpenemsException("Unable to set values [" + qCharacteristic + "] " + e.getMessage());
		}
	}

	@Override
	public void run() {
		float voltageRatio = meter.getVoltage().value().orElse(0) / this.nominalVoltage;
		float valueOfLine = Utils.getValueOfLine(qCharacteristic, voltageRatio);
		if (valueOfLine == 0) {
			return;
		} else {
			try {
				Value<Integer> apparentPower = ess.getMaxApparentPower().value();
				if (apparentPower.get() != null && apparentPower.get() != 0) {
					this.power = (int) (apparentPower.orElse(0) * valueOfLine);
					int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(ess, Phase.ALL, Pwr.REACTIVE,
							this.power);
					this.ess.addPowerConstraintAndValidate("ReactivePowerVoltageCharacteristic", Phase.ALL,
							Pwr.REACTIVE, Relationship.EQUALS, calculatedPower);
				} else {
					return;
				}
			} catch (PowerException e) {
				e.printStackTrace();
			}
		}
	}
}
