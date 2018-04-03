package tools.websocket;

public class SimpleWebsocketServer {

	private final static int PORT = 9080;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start SimpleWebsocketServer on port [" + PORT + "]");

		SimpleWebsocketServerImpl ws = new SimpleWebsocketServerImpl(PORT);
		ws.start();

		while (true) {
			Thread.sleep(5000);
			System.out.println("Still alive... Connections: " + ws.getConnections().size());
		}
	}
}
