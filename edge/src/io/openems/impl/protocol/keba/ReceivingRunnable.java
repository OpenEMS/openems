package io.openems.impl.protocol.keba;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ReceivingRunnable implements Runnable {

	private final int port;

	public ReceivingRunnable(int port) {
		log.info("Constr. ReceivingRunnable on " + port);
		this.port = port;
	}

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public void run() {

		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket(this.port);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (socket != null) {

			while (true) {
				try {
					log.info("-> receivingRunnable");
					// Auf Anfrage warten
					DatagramPacket packet = new DatagramPacket(new byte[512], 512);
					socket.receive(packet);

					// Empfänger auslesen
					InetAddress address = packet.getAddress();
					int port = packet.getPort();
					int len = packet.getLength();
					byte[] data = packet.getData();

					String message = new String(data, 0, len);
					log.info("Anfrage von " + address + " vom Port " + port + " mit der L�nge " + len + ": " + message);

					if (message.startsWith("TCH-OK")) {
						log.info("Command was received");
					} else {
						JsonParser parser = new JsonParser();
						JsonElement jMessageElement = parser.parse(message);
						JsonObject jMessage = jMessageElement.getAsJsonObject();
						String id = jMessage.get("ID").getAsString();
						log.info("Message: " + jMessage.toString());
						if (id.equals("1")) {
							log.info("Product: " + jMessage.get("Product").getAsString());
						}
					}
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
