package io.openems.edge.simulator.meter.grid.reacting;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.simulator.meter.MeterUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.GridMeter.Reacting", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GridMeter extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

	// private final Logger log = LoggerFactory.getLogger(GridMeter.class);

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SIMULATED_ACTIVE_POWER(new Doc().unit(Unit.WATT));
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<ManagedSymmetricEss> symmetricEsss = new CopyOnWriteArrayList<>();
	
	// all meters are needed even grid meters
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<SymmetricMeter> symmetricMeters = new CopyOnWriteArrayList<>();

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// Initialize Min/MaxActivePower channels
		this._initializeMinMaxActivePower(this.cm, config.service_pid(), config.minActivePower(),
				config.maxActivePower());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GridMeter() {
		MeterUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
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
		// calculate power sum from all meters and esss, but exclude grid meters.
		// Count the latter to spread the load equally on the different grid-nodes.
		int powerSum = 0;
		int gridCount = 0;
		
		for (ManagedSymmetricEss ess : this.symmetricEsss) {
			try {
				powerSum += ess.getActivePower().getNextValue().get();				
			} catch (NullPointerException e) {}
		}
		for (SymmetricMeter sm : this.symmetricMeters) {
			if (sm.getMeterType() != MeterType.GRID) {
				try {
					powerSum += (Integer) sm.getActivePower().getNextValue().get();
				} catch (NullPointerException e) {}
			} else {
				gridCount++;
			}
		}
		
		int activePower = -powerSum;
		// prevent division by 0 (occurs at startup of the first GridMeter)
		if (gridCount != 0) {
			// grids level the resulting power on 0
			activePower /= gridCount;
		}

		this.channel(ChannelId.SIMULATED_ACTIVE_POWER).setNextValue(activePower);
		this.getActivePower().setNextValue(activePower);
		this.getActivePowerL1().setNextValue(activePower / 3);
		this.getActivePowerL2().setNextValue(activePower / 3);
		this.getActivePowerL3().setNextValue(activePower / 3);
	}

	@Override
	public String debugLog() {
		return this.getActivePower().value().asString();
	}
}
