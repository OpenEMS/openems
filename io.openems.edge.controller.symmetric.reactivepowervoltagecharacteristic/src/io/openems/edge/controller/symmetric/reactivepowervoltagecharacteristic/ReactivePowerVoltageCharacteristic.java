package io.openems.edge.controller.symmetric.reactivepowervoltagecharacteristic;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
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

	private final Logger log = LoggerFactory.getLogger(ReactivePowerVoltageCharacteristic.class);

	private float nominalVoltage;
	private SymmetricMeter meter;
	private List<Point> qCharacteristic;
	private List<Long[]> qByUCharacteristicPoints;
	LongReadChannel maxNominalPower;

	public ReactivePowerVoltageCharacteristic(Config config) {
		initialize();
		this.nominalVoltage = config.nominalVoltage();
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}
		// Parsing JSon then putting the point variables into qByUCharacteristicEquation
		String percent = null, power = null;
		String percentQ = config.percentQ();
		JsonObject jsonObject = new JsonParser().parse(percentQ).getAsJsonObject();

		JsonArray arr = jsonObject.getAsJsonArray(percentQ);
		for (int i = 0; i < arr.size(); i++) {
			percent = arr.get(i).getAsJsonObject().get("percent").getAsString();
			power = arr.get(i).getAsJsonObject().get("power").getAsString();
		}
		System.out.println(percent + "---" + power);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private int power = 0;

	/*
	 * Methods
	 */
	private void initialize() {
		List<Point> points = new ArrayList<>();
		for (Long[] arr : qByUCharacteristicPoints) {
			points.add(new Point(arr[1].intValue(), arr[0].intValue()));
		}
		qCharacteristic = points;
	}

	@Override
	public void run() {
		double uRatio = meter.getVoltage().getNextValue().get() / nominalVoltage * 100.0;
		Channel<Integer> nominalActivePower = ess.getMaxApparentPower();

		int calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.REACTIVE, this.power);
		try {
			this.ess.addPowerConstraintAndValidate("ReactivePowerVoltageCharacteristic", Phase.ALL, Pwr.REACTIVE,
					Relationship.EQUALS, calculatedPower);
		} catch (PowerException e) {
			e.printStackTrace();
		}
	}
}
