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
	private SymmetricMeter meter;
	private Map<Double, Double> qCharacteristic = new HashMap<>();
	LongReadChannel maxNominalPower;

	@Reference
	protected ConfigurationAdmin cm;

	private int power = 0;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}
		this.nominalVoltage = config.nominalVoltage();
		initialize(config);
	}

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
		// [{ "power" : 0.9,"percent" : 60 }, { "power":0.93,"percent": 0}, {"power":
		// 1.07 ,"percent": 0 },{"power": 1.1 ,"percent": -60 }]
		// ...
		try {
			JsonElement jElement = JsonUtils.parse(config.percentQ());
			JsonArray jArray = JsonUtils.getAsJsonArray(jElement);
			JsonElement jEl;
			double percent = 0, power = 0;
			for (int i = 0; i < jArray.size(); i++) {
				jEl = jArray.get(i);
				percent = jEl.getAsJsonObject().get("percent").getAsDouble();
				power = jEl.getAsJsonObject().get("power").getAsDouble();
				qCharacteristic.put(power, percent);
			}
			System.out.println(qCharacteristic);
		} catch (NullPointerException | OpenemsException e) {
			throw new OpenemsException("Unable to set values [" + qCharacteristic + "] " + e.getMessage());
		}
	}

	@Override
	public void run() {
		double voltageRatio = (double) meter.getVoltage().value().get() / (double) this.nominalVoltage;
		double ratio = Utils.getValueOfLine(qCharacteristic, voltageRatio);
		Value<Integer> reactivePower = ess.getReactivePower().value();
		this.power = (int) (reactivePower.get().doubleValue() * ratio);
		int calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.REACTIVE, this.power);
		try {
			this.ess.addPowerConstraintAndValidate("ReactivePowerVoltageCharacteristic", Phase.ALL, Pwr.REACTIVE,
					Relationship.EQUALS, calculatedPower);
		} catch (PowerException e) {
			e.printStackTrace();
		}
	}
}
