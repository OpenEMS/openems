package io.openems.backend.edge.manager;

import java.net.InetSocketAddress;
import java.util.function.Function;

import io.openems.common.websocket.WebSocket;

@SuppressWarnings("unchecked")
public class DummyWebsocket implements WebSocket {

	private WsData attachment;

	public DummyWebsocket(Function<DummyWebsocket, WsData> wsDataSupplier) {
		this.attachment = wsDataSupplier.apply(this);
	}

	@Override
	public void send(String text) {
		// Not implemented for dummy
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public void close() {
		// Not implemented for dummy
	}

	@Override
	public void close(int code, String reason) {
		// Not implemented for dummy
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return null;
	}

	@Override
	public <T> void setAttachment(T attachment) {
		this.attachment = (WsData) attachment;
	}

	@Override
	public <T> T getAttachment() {
		return (T) this.attachment;
	}

	public WsData getWsData() {
		return this.attachment;
	}
}
