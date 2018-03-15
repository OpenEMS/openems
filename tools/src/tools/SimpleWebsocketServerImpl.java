package tools;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class SimpleWebsocketServerImpl extends WebSocketServer {

	public SimpleWebsocketServerImpl(int port) {
		super(new InetSocketAddress(port));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("On Open. Conn [" + conn + "]. Handshake [" + handshake + "]");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(
				"On Close. Conn [" + conn + "]. Code [" + code + "]. Reason [" + reason + "]. Remote [" + remote + "]");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println("On Message. Conn [" + conn + "]. Message [" + message + "]");
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.out.println("On Error. Conn [" + conn + "]. Exception [" + ex.getMessage() + "]");
	}

	@Override
	public void onStart() {
		System.out.println("On Start");
	}

}
