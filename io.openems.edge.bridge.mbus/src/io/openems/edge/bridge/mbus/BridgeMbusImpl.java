package io.openems.edge.bridge.mbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.MbusTask;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Mbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class BridgeMbusImpl extends AbstractOpenemsComponent implements BridgeMbus, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeMbusImpl.class);

	private final Map<String, MbusTask> tasks = new HashMap<>();
	private final MbusWorker worker = new MbusWorker();

	private MBusConnection mBusConnection;
	private MBusSerialBuilder builder;
	private String portName;

	public BridgeMbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeMbus.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.portName = config.portName();

		this.worker.activate(config.id());

		this.builder = MBusConnection.newSerialBuilder(this.portName).setBaudrate(config.baudrate());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.mBusConnection.close();
		this.worker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.worker.triggerNextRun();
			break;
		}
	}

	@Override
	public MBusConnection getmBusConnection() {
		return this.mBusConnection;
	}

	private class MbusWorker extends AbstractCycleWorker {

		@Override
		protected void forever() throws OpenemsException, DecodingException {
			// Check if time passed by, if not, do nothing
			try {
				BridgeMbusImpl.this.mBusConnection = BridgeMbusImpl.this.builder.build();

				for (MbusTask task : BridgeMbusImpl.this.tasks.values()) {
					try {
						var data = task.getRequest();
						data.decode();
						// "Before accessing elements of a variable data structure it has to be decoded
						// using the decode method." ??
						task.setResponse(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				BridgeMbusImpl.this.mBusConnection.close();
			} catch (IOException e) {
				BridgeMbusImpl.this.logError(BridgeMbusImpl.this.log,
						"Connection via [" + BridgeMbusImpl.this.portName + "] failed: " + e.getMessage());
			}
		}
	}

	@Override
	public void addTask(String sourceId, MbusTask task) {
		this.tasks.put(sourceId, task);
	}

	@Override
	public void removeTask(String sourceId) {
		this.tasks.remove(sourceId);
	}

}
