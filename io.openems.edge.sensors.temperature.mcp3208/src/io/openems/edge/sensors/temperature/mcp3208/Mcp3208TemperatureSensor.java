package io.openems.edge.sensors.temperature.mcp3208;

import com.pi4j.io.spi.SpiChannel;
import io.openems.edge.bridge.spi.api.Board;
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

import io.openems.edge.bridge.spi.api.BridgeSpi;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.sensors.temperature.api.TemperatureSensor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Sensor.Temperature.MCP3208", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Mcp3208TemperatureSensor extends AbstractOpenemsComponent implements TemperatureSensor, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	public Mcp3208TemperatureSensor(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
									io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected BridgeSpi spi;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'spi'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "spi", config.spi_id())) {
			return;
		}

		Position p = Position.valueOf(config.position());

		this.spi.addTask(config.id(),
				new Mcp3208DigitalReadTask(p.getSpiChannel(), p.getPort(), this.getTemperature(), Board.valueOf(config.board())));
	}

	@Deactivate
	protected void deactivate() {
		this.spi.removeTask(this.id());
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public String debugLog() {
		return "T:" + this.getTemperature().value().asString();
	}
}