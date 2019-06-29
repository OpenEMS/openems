package io.openems.edge.meter.discovergy;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Discovergy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //
public class MeterDiscovergy extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	private DiscovergyApiClient apiClient = null;
	private DiscovergyWorker worker = null;

	public MeterDiscovergy() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();

		super.activate(context, config.id(), config.alias(), config.enabled());

		if (config.enabled()) {
			DiscovergyApi api = new DiscovergyApi(config.email(), config.password());
			this.apiClient = new DiscovergyApiClient(api);

			this.worker = new DiscovergyWorker(this, apiClient, config.refreshPeriod(), config.meterId());
			this.worker.activate(config.id());
			this.worker.triggerNextRun();
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.worker != null) {
			this.worker.deactivate();
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REST_API_FAILED(Doc.of(Level.FAULT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}
}
