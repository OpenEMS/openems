package io.openems.edge.controller.pbyf;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.aithil.AitHil;
import io.openems.edge.aithil.AitHil.FreqWattCrv;
import io.openems.edge.aithil.AitHil.PByF;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.AIT.P-by-F", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PByFControllerImpl extends AbstractOpenemsComponent
		implements PByFController, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PByFControllerImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private AitHil aithil;

	private final TreeMap<ZonedDateTime, FreqWattCrv> schedule = new TreeMap<>();

	public PByFControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PByFController.ChannelId.values() //
		);
	}

	@Activate
	private synchronized void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'component'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "aithil", config.aithil_id())) {
			return;
		}

		// parse Schedule
		try {
			if (!config.schedule().trim().isEmpty()) {
				JsonArray schedule = JsonUtils.getAsJsonArray(JsonUtils.parse(config.schedule()));
				this.schedule.clear();
				this.schedule.putAll(parseSchedule(schedule));
			}
			this._setScheduleParseFailed(false);

		} catch (OpenemsNamedException e) {
			this._setScheduleParseFailed(true);
			this.logError(this.log, "Unable to parse Schedule: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Parse the Schedule to a TreeMap sorted by ZonedDateTime key.
	 * 
	 * @param schedule
	 * @return
	 * @throws OpenemsNamedException
	 */
	private static TreeMap<ZonedDateTime, FreqWattCrv> parseSchedule(JsonArray schedule) throws OpenemsNamedException {
		TreeMap<ZonedDateTime, FreqWattCrv> result = new TreeMap<>();
		long nextStartTimestamp = 0;

		for (JsonElement element : schedule) {
			JsonObject object = JsonUtils.getAsJsonObject(element);
			long startTimestamp = JsonUtils.getAsLong(object, "startTimestamp");
			int duration = JsonUtils.getAsInt(object, "duration");
			boolean enabled = JsonUtils.getAsBoolean(object, "enabled");
			JsonArray jCurve = JsonUtils.getAsJsonArray(object, "curve");
			PByF[] curve = new PByF[jCurve.size()];

			for (int i = 0; i < jCurve.size(); i++) {
				JsonObject point = JsonUtils.getAsJsonObject(jCurve.get(i));
				float f = JsonUtils.getAsFloat(point, "f");
				float p = JsonUtils.getAsFloat(point, "p");

				curve[i] = new PByF(p, f);
			}

			// Fill Gap?
			if (startTimestamp > nextStartTimestamp) {
				FreqWattCrv setting = new FreqWattCrv(false, new PByF[0]);
				ZonedDateTime start = ZonedDateTime.ofInstant(Instant.ofEpochSecond(nextStartTimestamp),
						ZoneId.of("UTC"));
				result.put(start, setting);
			}
			nextStartTimestamp = startTimestamp + duration;

			// Add to result
			FreqWattCrv setting = new FreqWattCrv(enabled, curve);
			ZonedDateTime start = ZonedDateTime.ofInstant(Instant.ofEpochSecond(startTimestamp), ZoneId.of("UTC"));
			result.put(start, setting);
		}

		// Final setting
		FreqWattCrv setting = new FreqWattCrv(false, new PByF[0]);
		ZonedDateTime start = ZonedDateTime.ofInstant(Instant.ofEpochSecond(nextStartTimestamp), ZoneId.of("UTC"));
		result.put(start, setting);

		return result;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ZonedDateTime now = ZonedDateTime.now(this.componentManager.getClock());
		Entry<ZonedDateTime, FreqWattCrv> entry = this.schedule.floorEntry(now);
		final FreqWattCrv freqWattCrv;
		if (entry == null) {
			freqWattCrv = new FreqWattCrv(false, new PByF[0]);
		} else {
			freqWattCrv = entry.getValue();
		}

		this.aithil.applyFreqWattCrv(freqWattCrv);
	}

}
