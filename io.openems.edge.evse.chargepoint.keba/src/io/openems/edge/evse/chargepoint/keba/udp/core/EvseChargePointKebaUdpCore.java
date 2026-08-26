package io.openems.edge.evse.chargepoint.keba.udp.core;

import java.net.InetAddress;
import java.util.function.BiConsumer;

public interface EvseChargePointKebaUdpCore {

	public static final int UDP_PORT = 7090;

	/**
	 * Callback on receive of a message. InetAddress is the address of the sending
	 * Charge-Point, String is the message.
	 *
	 * @param callback Callback
	 */
	public void onReceive(BiConsumer<InetAddress, String> callback);

}