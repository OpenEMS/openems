package io.openems.edge.simulator.meter.grid.reacting;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.GridMeter.Reacting", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class GridMeter extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

	// private final Logger log = LoggerFactory.getLogger(GridMeter.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT));

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
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
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
			} catch (NullPointerException e) {
				// ignore
			}
		}
		for (SymmetricMeter sm : this.symmetricMeters) {
			if (sm.getMeterType() != MeterType.GRID) {
				try {
					powerSum += (Integer) sm.getActivePower().getNextValue().get();
				} catch (NullPointerException e) {
					// ignore
				}
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
