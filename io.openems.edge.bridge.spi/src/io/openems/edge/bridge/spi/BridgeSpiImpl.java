package io.openems.edge.bridge.spi;

import com.pi4j.gpio.extension.mcp.MCP3208GpioProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Designate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.spi.SpiChannel;
import com.pi4j.wiringpi.Spi;

import io.openems.edge.bridge.spi.api.BridgeSpi;
import io.openems.edge.bridge.spi.task.Task;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.common.worker.AbstractCycleWorker;


@Designate(ocd = Config.class, factory = false)
@Component(name = "Bridge.Spi",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeSpiImpl extends AbstractOpenemsComponent implements BridgeSpi, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeSpiImpl.class);

	private final Map<String, Task> tasks = new ConcurrentHashMap<>();
	private final SpiWorker worker = new SpiWorker();


	protected BridgeSpiImpl(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
								   io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}


	@Activate
	protected void activate(ComponentContext context, Config config)  {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		if (this.isEnabled()) {
			this.worker.activate(config.id());
		}
		//doc https://github.com/Pi4J/pi4j/blob/master/pi4j-core/src/main/java/com/pi4j/wiringpi/Spi.java
		// https://github.com/Pi4J/pi4j/blob/master/pi4j-core/src/main/java/com/pi4j/io/spi/SpiChannel.java
		Spi.wiringPiSPISetup(config.cs_0(), config.frequency());
		Spi.wiringPiSPISetup(config.cs_1(), config.frequency());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	@Override
	public void addTask(String sourceId, Task task) {
		this.tasks.put(sourceId, task);
	}

	@Override
	public void removeTask(String sourceId) {
		this.tasks.remove(sourceId);
	}

	private class SpiWorker extends AbstractCycleWorker {

		@Override
		public void activate(String name) {
			super.activate(name);
		}

		@Override
		public void deactivate() {
			super.deactivate();
		}

		@Override
		protected void forever() {
			for (Task task : tasks.values()) {
				byte[] data = task.getRequest();
				Spi.wiringPiSPIDataRW(task.getChannel().getChannel(), data);
				task.setResponse(data);
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
				this.worker.triggerNextRun();
				break;
		}
	}
}