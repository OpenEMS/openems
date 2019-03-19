package io.openems.edge.evcs.keba.kecontact;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.keba.kecontact.core.KebaKeContactCore;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Keba.KeContact", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class KebaKeContact extends AbstractOpenemsComponent implements Evcs, OpenemsComponent, EventHandler {

	public final static int UDP_PORT = 7090;

	private final Logger log = LoggerFactory.getLogger(KebaKeContact.class);
	private final ReadWorker readWorker = new ReadWorker(this);
	private final ReadHandler readHandler = new ReadHandler(this);
	private final WriteHandler writeHandler = new WriteHandler(this);

	@Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	private KebaKeContactCore kebaKeContactCore = null;

	public KebaKeContact() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				KebaChannelId.values() //
		);
	}

	private InetAddress ip = null;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.enabled());

		this.ip = Inet4Address.getByName(config.ip());

		/*
		 * subscribe on replies to report queries
		 */
		this.kebaKeContactCore.onReceive((ip, message) -> {
			if (ip.equals(this.ip)) { // same IP -> handle message
				this.readHandler.accept(message);
			}
		});

		// start queryWorker
		this.readWorker.activate(this.id() + "query");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.readWorker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			// handle writes
			this.writeHandler.run();
			break;
		}
	}

	/**
	 * Send UDP message to KEBA KeContact. Returns true if sent successfully
	 *
	 * @param s
	 * @return
	 */
	protected boolean send(String s) {
		byte[] raw = s.getBytes();
		DatagramPacket packet = new DatagramPacket(raw, raw.length, ip, KebaKeContact.UDP_PORT);
		DatagramSocket dSocket = null;
		try {
			dSocket = new DatagramSocket();
			this.logInfo(this.log, "Sending message to KEBA KeContact [" + s + "]");
			dSocket.send(packet);
			return true;
		} catch (SocketException e) {
			this.logError(this.log, "Unable to open UDP socket for sending [" + s + "] to [" + ip.getHostAddress()
					+ "]: " + e.getMessage());
		} catch (IOException e) {
			this.logError(this.log,
					"Unable to send [" + s + "] UDP message to [" + ip.getHostAddress() + "]: " + e.getMessage());
		} finally {
			if (dSocket != null) {
				dSocket.close();
			}
		}
		return false;
	}

	/**
	 * Triggers an immediate execution of query reports
	 */
	protected void triggerQuery() {
		this.readWorker.triggerNextRun();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.channel(KebaChannelId.CURR_USER).value().asString();
	}

	public ReadWorker getReadWorker() {
		return readWorker;
	}

	public ReadHandler getReadHandler() {
		return readHandler;
	}
}
