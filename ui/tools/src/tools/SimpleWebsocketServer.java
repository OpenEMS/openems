package tools;

public class SimpleWebsocketServer {
	
	private final static int PORT = 9080;
	
	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start SimpleWebsocketServer on port [" + PORT + "]");
		
		SimpleWebsocketServerImpl ws = new SimpleWebsocketServerImpl(PORT);
		ws.start();
		Thread.sleep(10000);
	}
}
