package io.openems.impl.protocol.keba;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.OpenemsException;

public class ReceiveWorker implements Runnable {

	public interface OnReceive {
		public void run(InetAddress ip, String message);
	}

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private Optional<OnReceive> onReceiveOpt = Optional.empty();
	private final DatagramSocket socket;

	public ReceiveWorker(int port) throws OpenemsException {
		try {
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			throw new OpenemsException(
					"Unable to open port [" + port + "] to receive UDP messages from KEBA KeContact: " + e.getMessage(),
					e);
		}
	}

	public ReceiveWorker onReceive(OnReceive onReceive) {
		this.onReceiveOpt = Optional.of(onReceive);
		return this;
	}

	@Override
	public void run() {
		while (true) {
			try {
				// Wait for message
				DatagramPacket packet = new DatagramPacket(new byte[512], 512);
				socket.receive(packet);

				// Read message
				InetAddress ip = packet.getAddress();
				int len = packet.getLength();
				byte[] data = packet.getData();

				String message = new String(data, 0, len);

				if (this.onReceiveOpt.isPresent()) {
					this.onReceiveOpt.get().run(ip, message);
				}

			} catch (Throwable e) {
				log.error("Error while receiving data from KEBA: " + e.getMessage());
			}
		}
	}
}
