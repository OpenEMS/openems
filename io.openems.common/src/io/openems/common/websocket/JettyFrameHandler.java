package io.openems.common.websocket;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.CloseStatus;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.FrameHandler;
import org.eclipse.jetty.websocket.core.OpCode;

/**
 * Jetty {@link FrameHandler} that assembles text messages, answers pings and
 * forwards the events of one connection to a {@link Listener}.
 */
final class JettyFrameHandler implements FrameHandler {

	/**
	 * Receives the events of a connection.
	 */
	interface Listener {

		/**
		 * The connection was opened.
		 */
		void onOpen();

		/**
		 * A complete text message was received.
		 *
		 * @param text the message
		 */
		void onText(String text);

		/**
		 * An error occurred; {@link #onClose(int, String, boolean)} follows.
		 *
		 * @param cause the cause
		 */
		void onError(Exception cause);

		/**
		 * The connection was closed.
		 *
		 * @param code   the close code
		 * @param reason the close reason
		 * @param remote true if the remote endpoint initiated the close
		 */
		void onClose(int code, String reason, boolean remote);
	}

	private final JettyWebSocket ws;
	private final Listener listener;

	private CoreSession session;
	/** Buffer for a fragmented text message; null if none is in progress. */
	private ByteArrayOutputStream textBuffer;
	private boolean binaryInProgress;

	JettyFrameHandler(JettyWebSocket ws, Listener listener) {
		this.ws = ws;
		this.listener = listener;
	}

	@Override
	public void onOpen(CoreSession session, Callback callback) {
		this.session = session;
		this.ws.bind(session);
		callback.succeeded();
		this.listener.onOpen();
		session.demand();
	}

	@Override
	public void onFrame(Frame frame, Callback callback) {
		this.ws.onFrameReceived();
		switch (frame.getOpCode()) {
		case OpCode.TEXT -> this.onTextFrame(frame);
		case OpCode.BINARY -> this.binaryInProgress = !frame.isFin();
		case OpCode.CONTINUATION -> this.onContinuationFrame(frame);
		case OpCode.PING -> this.session.sendFrame(new Frame(OpCode.PONG).setPayload(copy(frame.getPayload())),
				Callback.NOOP, false);
		case OpCode.CLOSE -> {
			// Close handshake is handled by the CoreSession
			callback.succeeded();
			return;
		}
		default -> {
			// PONG only updates the last received timestamp
		}
		}
		callback.succeeded();
		this.session.demand();
	}

	private void onTextFrame(Frame frame) {
		if (frame.isFin()) {
			this.listener.onText(frame.getPayloadAsUTF8());
			return;
		}
		this.textBuffer = new ByteArrayOutputStream();
		append(this.textBuffer, frame.getPayload());
	}

	private void onContinuationFrame(Frame frame) {
		if (this.binaryInProgress) {
			this.binaryInProgress = !frame.isFin();
			return;
		}
		if (this.textBuffer == null) {
			return;
		}
		append(this.textBuffer, frame.getPayload());
		if (frame.isFin()) {
			// Decode only the complete message: UTF-8 sequences may span frames
			var text = this.textBuffer.toString(StandardCharsets.UTF_8);
			this.textBuffer = null;
			this.listener.onText(text);
		}
	}

	@Override
	public void onError(Throwable cause, Callback callback) {
		this.listener.onError(cause instanceof Exception e ? e : new Exception(cause));
		callback.succeeded();
	}

	@Override
	public void onClosed(CloseStatus closeStatus, Callback callback) {
		this.ws.unbind(this.session);
		this.textBuffer = null;
		this.listener.onClose(closeStatus.getCode(), closeStatus.getReason(), !this.ws.isClosedLocally());
		callback.succeeded();
	}

	private static void append(ByteArrayOutputStream out, ByteBuffer payload) {
		if (payload == null) {
			return;
		}
		var buffer = payload.slice();
		var bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		out.writeBytes(bytes);
	}

	private static ByteBuffer copy(ByteBuffer payload) {
		if (payload == null) {
			return null;
		}
		var copy = ByteBuffer.allocate(payload.remaining());
		copy.put(payload.slice());
		return copy.flip();
	}
}
