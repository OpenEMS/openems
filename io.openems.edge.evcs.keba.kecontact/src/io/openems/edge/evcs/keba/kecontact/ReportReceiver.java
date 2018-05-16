package io.openems.edge.evcs.keba.kecontact;

import java.net.InetAddress;
import java.util.function.BiConsumer;

/**
 * Handles replys to Report Querys sent by {@link QueryWorker}
 *
 */
public class ReportReceiver implements BiConsumer<InetAddress, String> {

	private final KebaKeContact parent;

	public ReportReceiver(KebaKeContact parent) {
		this.parent = parent;
	}

	@Override
	public void accept(InetAddress ip, String message) {
		System.out.println("Message from [" + ip + "]: " + message);
	}
}
