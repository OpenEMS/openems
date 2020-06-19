package io.openems.edge.evcs.keba.kecontact.core;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.evcs.keba.kecontact.KebaKeContact;

@Component(//
		name = "Evcs.Keba.KeContact.Core", //
		immediate = false)
public class KebaKeContactCoreImpl implements KebaKeContactCore {

	private final Logger log = LoggerFactory.getLogger(KebaKeContactCoreImpl.class);
	private final List<BiConsumer<InetAddress, String>> onReceiveCallbacks = new CopyOnWriteArrayList<>();
	private ReceiveWorker receiveWorker = null;

	@Activate
	void activate() throws OpenemsException {
		this.receiveWorker = new ReceiveWorker(KebaKeContact.UDP_PORT);
		this.receiveWorker.activate("kebaCore");
	}

	@Deactivate
	protected void deactivate() {
		if (this.receiveWorker != null) {
			this.receiveWorker.deactivate();
		}
	}

	@Override
	public void onReceive(BiConsumer<InetAddress, String> callback) {
		this.onReceiveCallbacks.add(callback);
	}

	private class ReceiveWorker extends AbstractImmediateWorker {

		private DatagramSocket socket;

		private ReceiveWorker(int port) throws OpenemsException {
			try {
				this.socket = new DatagramSocket(port);
			} catch (SocketException e) {
				throw new OpenemsException("Unable to open port [" + port
						+ "] to receive UDP messages from KEBA KeContact. " + e.getMessage());
			}
		}

		@Override
		public void activate(String name) {
			super.activate(name);
			log.info("Started Evcs.Keba.KeContact.Core listener on port [" + KebaKeContact.UDP_PORT + "]");
		}

		@Override
		public void deactivate() {
			this.socket.close();
			super.deactivate();
			log.info("Stopped Evcs.Keba.KeContact.Core listener on port [" + KebaKeContact.UDP_PORT + "]");
		}

		@Override
		protected void forever() {
			// Wait for message
			DatagramPacket packet = new DatagramPacket(new byte[512], 512);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				log.error("Error while receiving data from KEBA KeContact: " + e.getMessage());
			}

			// Read message
			InetAddress ip = packet.getAddress();
			int len = packet.getLength();
			byte[] data = packet.getData();
			String message = new String(data, 0, len);

			// call callbacks
			onReceiveCallbacks.forEach(consumer -> consumer.accept(ip, message));
		}
	}

}
