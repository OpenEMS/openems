package io.openems.common.websocket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.OpCode;

/**
 * {@link WebSocket} implementation on top of a Jetty {@link CoreSession}.
 *
 * <p>
 * A client keeps one instance across reconnects; a server creates one instance
 * per connection.
 */
final class JettyWebSocket implements WebSocket {

	/** Max length of a close reason in bytes, see RFC 6455 section 5.5. */
	private static final int MAX_CLOSE_REASON_BYTES = 123;

	private volatile Object attachment;
	private volatile CoreSession session;
	private volatile InetSocketAddress remoteAddress;
	private volatile boolean closedLocally;
	private volatile long lastReceivedNanos;
	private volatile long lastPingNanos;

	JettyWebSocket(SocketAddress remoteAddress) {
		this.remoteAddress = toInetSocketAddress(remoteAddress);
	}

	/**
	 * Binds an opened {@link CoreSession} to this {@link WebSocket}.
	 *
	 * @param session the {@link CoreSession}
	 */
	void bind(CoreSession session) {
		var now = System.nanoTime();
		this.lastReceivedNanos = now;
		this.lastPingNanos = now;
		this.closedLocally = false;
		var address = toInetSocketAddress(session.getRemoteAddress());
		if (address != null) {
			this.remoteAddress = address;
		}
		this.session = session;
	}

	/**
	 * Unbinds a closed {@link CoreSession}.
	 *
	 * @param session the {@link CoreSession}
	 */
	void unbind(CoreSession session) {
		if (this.session == session) {
			this.session = null;
		}
	}

	/**
	 * Was the connection closed by this endpoint?.
	 *
	 * @return true if {@link #close(int, String)} was called
	 */
	boolean isClosedLocally() {
		return this.closedLocally;
	}

	/**
	 * Marks that a frame was received from the remote endpoint.
	 */
	void onFrameReceived() {
		this.lastReceivedNanos = System.nanoTime();
	}

	/**
	 * Detects a lost connection: sends a ping every {@code timeout} and aborts the
	 * connection if nothing was received for 1.5 times {@code timeout}.
	 *
	 * <p>
	 * May be called more often than {@code timeout}.
	 *
	 * @param timeout the connection lost timeout
	 * @return true if the connection was aborted
	 */
	boolean checkConnectionLost(Duration timeout) {
		var session = this.session;
		if (session == null || !session.isOutputOpen()) {
			return false;
		}
		var now = System.nanoTime();
		var timeoutNanos = timeout.toNanos();
		if (now - this.lastPingNanos < timeoutNanos) {
			return false;
		}
		this.lastPingNanos = now;
		if (now - this.lastReceivedNanos > timeoutNanos * 3 / 2) {
			session.abort();
			return true;
		}
		session.sendFrame(new Frame(OpCode.PING), Callback.NOOP, false);
		return false;
	}

	/**
	 * Aborts the connection without a close handshake.
	 */
	void abort() {
		var session = this.session;
		if (session != null) {
			session.abort();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAttachment() {
		return (T) this.attachment;
	}

	@Override
	public <T> void setAttachment(T attachment) {
		this.attachment = attachment;
	}

	@Override
	public void send(String text) {
		var session = this.session;
		if (session == null) {
			return;
		}
		session.sendFrame(new Frame(OpCode.TEXT, text), Callback.NOOP, false);
	}

	@Override
	public boolean isOpen() {
		var session = this.session;
		return session != null && session.isOutputOpen();
	}

	@Override
	public void close() {
		this.close(CloseCode.NORMAL, null);
	}

	@Override
	public void close(int code, String reason) {
		this.close(code, reason, Callback.NOOP);
	}

	private void close(int code, String reason, Callback callback) {
		var session = this.session;
		if (session == null) {
			callback.succeeded();
			return;
		}
		this.closedLocally = true;
		if (code < CloseCode.NORMAL || code == CloseCode.ABNORMAL_CLOSE) {
			// Codes that must not be sent in a close frame
			session.abort();
			callback.succeeded();
			return;
		}
		session.close(code, truncateCloseReason(reason), callback);
	}

	/**
	 * Closes the connection and waits till the close handshake is completed.
	 *
	 * @param code    the close code, see {@link CloseCode}
	 * @param reason  the close reason
	 * @param timeout the max time to wait
	 */
	void closeAndWait(int code, String reason, Duration timeout) {
		var callback = new FutureCallback();
		this.close(code, reason, callback);
		try {
			callback.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException e) {
			// Connection is aborted when the client is stopped
		}
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return this.remoteAddress;
	}

	/**
	 * Converts HTTP headers to a {@link Handshake}. Repeated headers are joined
	 * with a comma, see RFC 9110 section 5.3.
	 *
	 * @param fields the HTTP headers
	 * @return the {@link Handshake}
	 */
	static Handshake toHandshake(Iterable<HttpField> fields) {
		var headers = new HashMap<String, String>();
		for (var field : fields) {
			headers.merge(field.getName(), field.getValue(), (a, b) -> a + ", " + b);
		}
		return Handshake.of(headers);
	}

	private static InetSocketAddress toInetSocketAddress(SocketAddress address) {
		return address instanceof InetSocketAddress isa ? isa : null;
	}

	static String truncateCloseReason(String reason) {
		if (reason == null) {
			return null;
		}
		var bytes = reason.getBytes(StandardCharsets.UTF_8);
		if (bytes.length <= MAX_CLOSE_REASON_BYTES) {
			return reason;
		}
		// Cut at a character boundary
		var length = MAX_CLOSE_REASON_BYTES;
		while (length > 0 && (bytes[length] & 0xC0) == 0x80) {
			length--;
		}
		return new String(bytes, 0, length, StandardCharsets.UTF_8);
	}
}
