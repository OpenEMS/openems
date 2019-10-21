package io.openems.edge.bridge.mbus;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.EventConstants;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.task.MbusTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusSerialBuilder;
import org.openmuc.jmbus.VariableDataStructure;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Mbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeMbusImpl extends AbstractOpenemsComponent implements BridgeMbus, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeMbusImpl.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public BridgeMbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeMbus.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	protected MBusConnection mBusConnection;
	private final Map<String, MbusTask> tasks = new HashMap<>();
	boolean isConnected;

	private final MbusWorker worker = new MbusWorker();
	private MBusSerialBuilder builder;

	public MBusConnection getmBusConnection() {
		return mBusConnection;
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.worker.activate(config.id());

		this.builder = MBusConnection.newSerialBuilder(config.device()).setBaudrate(config.baud());

		try {
			mBusConnection = builder.build();

			isConnected = true;
			mBusConnection.close();
			System.out.println("Connection established");
		} catch (Exception e) {
			log.debug("Connection could not be established {0}", e.getMessage());
			// logger.log(Level.ALL, "Connection could not be established {0}",
			// e.getMessage());
			System.out.println(e.getMessage());
			isConnected = false;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		mBusConnection.close();
		this.worker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.worker.triggerNextRun();
			break;
		}
	}

	private class MbusWorker extends AbstractCycleWorker {

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
			// Check if time passed by, if not, do nothing
			try {
				mBusConnection = builder.build();

				for (MbusTask task : tasks.values()) {
					VariableDataStructure data = null;
					try {
						data = task.getRequest();
						task.setResponse(data);
					} catch (InterruptedIOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				mBusConnection.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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
