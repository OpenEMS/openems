package io.openems.edge.bridge.mccomms.task;

import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;

public class WriteTask {
	private int sourceAddress;
	private int destinationAddress;
	private int command;
	private MCCommsPacket packet;
}
