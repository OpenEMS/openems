package io.openems.backend.edge.manager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.function.Function;

import javax.net.ssl.SSLSession;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;

@SuppressWarnings("unchecked")
public class DummyWebsocket implements WebSocket {

	private WsData attachment;

	public DummyWebsocket(Function<DummyWebsocket, WsData> wsDataSupplier) {
		this.attachment = wsDataSupplier.apply(this);
	}

	@Override
	public void close(int i, String s) {
		// Not implemented for dummy
	}

	@Override
	public void close(int i) {
		// Not implemented for dummy
	}

	@Override
	public void close() {
		// Not implemented for dummy
	}

	@Override
	public void closeConnection(int i, String s) {
		// Not implemented for dummy
	}

	@Override
	public void send(String s) {
		// Not implemented for dummy
	}

	@Override
	public void send(ByteBuffer byteBuffer) {
		// Not implemented for dummy
	}

	@Override
	public void send(byte[] bytes) {
		// Not implemented for dummy
	}

	@Override
	public void sendFrame(Framedata framedata) {
		// Not implemented for dummy
	}

	@Override
	public void sendFrame(Collection<Framedata> collection) {
		// Not implemented for dummy
	}

	@Override
	public void sendPing() {
		// Not implemented for dummy
	}

	@Override
	public void sendFragmentedFrame(Opcode opcode, ByteBuffer byteBuffer, boolean b) {
		// Not implemented for dummy
	}

	@Override
	public boolean hasBufferedData() {
		return false;
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return null;
	}

	@Override
	public InetSocketAddress getLocalSocketAddress() {
		return null;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public boolean isClosing() {
		return false;
	}

	@Override
	public boolean isFlushAndClose() {
		return false;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public Draft getDraft() {
		return null;
	}

	@Override
	public ReadyState getReadyState() {
		return null;
	}

	@Override
	public String getResourceDescriptor() {
		return "";
	}

	@Override
	public <T> void setAttachment(T t) {
		this.attachment = (WsData) t;
	}

	@Override
	public <T> T getAttachment() {
		return (T) this.attachment;
	}

	public WsData getWsData() {
		return this.attachment;
	}

	@Override
	public boolean hasSSLSupport() {
		return false;
	}

	@Override
	public SSLSession getSSLSession() throws IllegalArgumentException {
		return null;
	}

	@Override
	public IProtocol getProtocol() {
		return null;
	}
}
