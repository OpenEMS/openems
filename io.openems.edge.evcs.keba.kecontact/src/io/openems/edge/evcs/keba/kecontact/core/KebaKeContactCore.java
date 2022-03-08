package io.openems.edge.evcs.keba.kecontact.core;

import java.net.InetAddress;
import java.util.function.BiConsumer;

public interface KebaKeContactCore {

	/**
	 * Callback on receive of a message. InetAddress is the address of the sending
	 * EVCS, String is the message.
	 *
	 * @param callback Callback
	 */
	public void onReceive(BiConsumer<InetAddress, String> callback);

}
