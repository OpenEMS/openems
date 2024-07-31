package io.openems.common.websocket;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.CloseHandshakeType;
import org.java_websocket.enums.HandshakeState;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.enums.Role;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidFrameException;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.exceptions.LimitExceededException;
import org.java_websocket.exceptions.NotSendableException;
import org.java_websocket.extensions.DefaultExtension;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.framing.BinaryFrame;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.FramedataImpl1;
import org.java_websocket.framing.TextFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ClientHandshakeBuilder;
import org.java_websocket.handshake.HandshakeBuilder;
import org.java_websocket.handshake.Handshakedata;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;
import org.java_websocket.util.Base64;
import org.java_websocket.util.Charsetfunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

/**
 * This class is basically a copy of the class {@link Draft_6455}. The only
 * difference is that it does not throw a IncompleteException internally instead
 * it returns normally in those cases and checks afterwards with an "if" if the
 * result is correct.
 */
public class MyDraft6455 extends Draft {

	private static class Either<LEFT, RIGHT> {
		public final LEFT left;
		public final RIGHT right;

		public static <LEFT, RIGHT> Either<LEFT, RIGHT> right(RIGHT right) {
			return new Either<LEFT, RIGHT>(null, Objects.requireNonNull(right));
		}

		public static <LEFT, RIGHT> Either<LEFT, RIGHT> left(LEFT left) {
			return new Either<LEFT, RIGHT>(Objects.requireNonNull(left), null);
		}

		private Either(LEFT left, RIGHT right) {
			super();
			this.left = left;
			this.right = right;
		}

	}

	/**
	 * Handshake specific field for the key.
	 */
	private static final String SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key";

	/**
	 * Handshake specific field for the protocol.
	 */
	private static final String SEC_WEB_SOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

	/**
	 * Handshake specific field for the extension.
	 */
	private static final String SEC_WEB_SOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

	/**
	 * Handshake specific field for the accept.
	 */
	private static final String SEC_WEB_SOCKET_ACCEPT = "Sec-WebSocket-Accept";

	/**
	 * Handshake specific field for the upgrade.
	 */
	private static final String UPGRADE = "Upgrade";

	/**
	 * Handshake specific field for the connection.
	 */
	private static final String CONNECTION = "Connection";

	private final Logger log = LoggerFactory.getLogger(MyDraft6455.class);

	/**
	 * Attribute for the used extension in this draft.
	 */
	private IExtension negotiatedExtension = new DefaultExtension();

	/**
	 * Attribute for the default extension.
	 */
	private IExtension defaultExtension = new DefaultExtension();

	/**
	 * Attribute for all available extension in this draft.
	 */
	private List<IExtension> knownExtensions;

	/**
	 * Current active extension used to decode messages.
	 */
	private IExtension currentDecodingExtension;

	/**
	 * Attribute for the used protocol in this draft.
	 */
	private IProtocol protocol;

	/**
	 * Attribute for all available protocols in this draft.
	 */
	private List<IProtocol> knownProtocols;

	/**
	 * Attribute for the current continuous frame.
	 */
	private Framedata currentContinuousFrame;

	/**
	 * Attribute for the payload of the current continuous frame.
	 */
	private final List<ByteBuffer> byteBufferList;

	/**
	 * Attribute for the current incomplete frame.
	 */
	private ByteBuffer incompleteframe;

	/**
	 * Attribute for the reusable random instance.
	 */
	private final SecureRandom reuseableRandom = new SecureRandom();

	/**
	 * Attribute for the maximum allowed size of a frame.
	 *
	 * @since 1.4.0
	 */
	private int maxFrameSize;

	/**
	 * Constructor for the websocket protocol specified by RFC 6455 with default
	 * extensions.
	 *
	 * @since 1.3.5
	 */
	public MyDraft6455() {
		this(Collections.<IExtension>emptyList());
	}

	/**
	 * Constructor for the websocket protocol specified by RFC 6455 with custom
	 * extensions.
	 *
	 * @param inputExtension the extension which should be used for this draft
	 * @since 1.3.5
	 */
	public MyDraft6455(IExtension inputExtension) {
		this(Collections.singletonList(inputExtension));
	}

	/**
	 * Constructor for the websocket protocol specified by RFC 6455 with custom
	 * extensions.
	 *
	 * @param inputExtensions the extensions which should be used for this draft
	 * @since 1.3.5
	 */
	public MyDraft6455(List<IExtension> inputExtensions) {
		this(inputExtensions, Collections.<IProtocol>singletonList(new Protocol("")));
	}

	/**
	 * Constructor for the websocket protocol specified by RFC 6455 with custom
	 * extensions and protocols.
	 *
	 * @param inputExtensions the extensions which should be used for this draft
	 * @param inputProtocols  the protocols which should be used for this draft
	 * @since 1.3.7
	 */
	public MyDraft6455(List<IExtension> inputExtensions, List<IProtocol> inputProtocols) {
		this(inputExtensions, inputProtocols, Integer.MAX_VALUE);
	}

	/**
	 * Constructor for the websocket protocol specified by RFC 6455 with custom
	 * extensions and protocols.
	 *
	 * @param inputExtensions   the extensions which should be used for this draft
	 * @param inputMaxFrameSize the maximum allowed size of a frame (the real
	 *                          payload size, decoded frames can be bigger)
	 * @since 1.4.0
	 */
	public MyDraft6455(List<IExtension> inputExtensions, int inputMaxFrameSize) {
		this(inputExtensions, Collections.<IProtocol>singletonList(new Protocol("")), inputMaxFrameSize);
	}

	/**
	 * Constructor for the websocket protocol specified by RFC 6455 with custom
	 * extensions and protocols.
	 *
	 * @param inputExtensions   the extensions which should be used for this draft
	 * @param inputProtocols    the protocols which should be used for this draft
	 * @param inputMaxFrameSize the maximum allowed size of a frame (the real
	 *                          payload size, decoded frames can be bigger)
	 * @since 1.4.0
	 */
	public MyDraft6455(List<IExtension> inputExtensions, List<IProtocol> inputProtocols, int inputMaxFrameSize) {
		if (inputExtensions == null || inputProtocols == null || inputMaxFrameSize < 1) {
			throw new IllegalArgumentException();
		}
		this.knownExtensions = new ArrayList<>(inputExtensions.size());
		this.knownProtocols = new ArrayList<>(inputProtocols.size());
		boolean hasDefault = false;
		this.byteBufferList = new ArrayList<>();
		for (IExtension inputExtension : inputExtensions) {
			if (inputExtension.getClass().equals(DefaultExtension.class)) {
				hasDefault = true;
			}
		}
		this.knownExtensions.addAll(inputExtensions);
		// We always add the DefaultExtension to implement the normal RFC 6455
		// specification
		if (!hasDefault) {
			this.knownExtensions.add(this.knownExtensions.size(), this.negotiatedExtension);
		}
		this.knownProtocols.addAll(inputProtocols);
		this.maxFrameSize = inputMaxFrameSize;
		this.currentDecodingExtension = null;
	}

	@Override
	public HandshakeState acceptHandshakeAsServer(ClientHandshake handshakedata) throws InvalidHandshakeException {
		int v = readVersionFrom(handshakedata);
		if (v != 13) {
			this.log.trace("acceptHandshakeAsServer - Wrong websocket version.");
			return HandshakeState.NOT_MATCHED;
		}
		HandshakeState extensionState = HandshakeState.NOT_MATCHED;
		String requestedExtension = handshakedata.getFieldValue(SEC_WEB_SOCKET_EXTENSIONS);
		for (IExtension knownExtension : this.knownExtensions) {
			if (knownExtension.acceptProvidedExtensionAsServer(requestedExtension)) {
				this.negotiatedExtension = knownExtension;
				extensionState = HandshakeState.MATCHED;
				this.log.trace("acceptHandshakeAsServer - Matching extension found: {}", this.negotiatedExtension);
				break;
			}
		}
		HandshakeState protocolState = this
				.containsRequestedProtocol(handshakedata.getFieldValue(SEC_WEB_SOCKET_PROTOCOL));
		if (protocolState == HandshakeState.MATCHED && extensionState == HandshakeState.MATCHED) {
			return HandshakeState.MATCHED;
		}
		this.log.trace("acceptHandshakeAsServer - No matching extension or protocol found.");
		return HandshakeState.NOT_MATCHED;
	}

	private static int readVersionFrom(Handshakedata handshakedata) {
		String vers = handshakedata.getFieldValue("Sec-WebSocket-Version");
		final var i = Ints.tryParse(vers);
		if (i != null) {
			return i;
		}
		return -1;
	}

	/**
	 * Check if the requested protocol is part of this draft.
	 *
	 * @param requestedProtocol the requested protocol
	 * @return MATCHED if it is matched, otherwise NOT_MATCHED
	 */
	private HandshakeState containsRequestedProtocol(String requestedProtocol) {
		for (IProtocol knownProtocol : this.knownProtocols) {
			if (knownProtocol.acceptProvidedProtocol(requestedProtocol)) {
				this.protocol = knownProtocol;
				this.log.trace("acceptHandshake - Matching protocol found: {}", this.protocol);
				return HandshakeState.MATCHED;
			}
		}
		return HandshakeState.NOT_MATCHED;
	}

	@Override
	public HandshakeState acceptHandshakeAsClient(ClientHandshake request, ServerHandshake response)
			throws InvalidHandshakeException {
		if (!basicAccept(response)) {
			this.log.trace("acceptHandshakeAsClient - Missing/wrong upgrade or connection in handshake.");
			return HandshakeState.NOT_MATCHED;
		}
		if (!request.hasFieldValue(SEC_WEB_SOCKET_KEY) || !response.hasFieldValue(SEC_WEB_SOCKET_ACCEPT)) {
			this.log.trace("acceptHandshakeAsClient - Missing Sec-WebSocket-Key or Sec-WebSocket-Accept");
			return HandshakeState.NOT_MATCHED;
		}

		String seckeyAnswer = response.getFieldValue(SEC_WEB_SOCKET_ACCEPT);
		String seckeyChallenge = request.getFieldValue(SEC_WEB_SOCKET_KEY);
		seckeyChallenge = this.generateFinalKey(seckeyChallenge);

		if (!seckeyChallenge.equals(seckeyAnswer)) {
			this.log.trace("acceptHandshakeAsClient - Wrong key for Sec-WebSocket-Key.");
			return HandshakeState.NOT_MATCHED;
		}
		HandshakeState extensionState = HandshakeState.NOT_MATCHED;
		String requestedExtension = response.getFieldValue(SEC_WEB_SOCKET_EXTENSIONS);
		for (IExtension knownExtension : this.knownExtensions) {
			if (knownExtension.acceptProvidedExtensionAsClient(requestedExtension)) {
				this.negotiatedExtension = knownExtension;
				extensionState = HandshakeState.MATCHED;
				this.log.trace("acceptHandshakeAsClient - Matching extension found: {}", this.negotiatedExtension);
				break;
			}
		}
		HandshakeState protocolState = this.containsRequestedProtocol(response.getFieldValue(SEC_WEB_SOCKET_PROTOCOL));
		if (protocolState == HandshakeState.MATCHED && extensionState == HandshakeState.MATCHED) {
			return HandshakeState.MATCHED;
		}
		this.log.trace("acceptHandshakeAsClient - No matching extension or protocol found.");
		return HandshakeState.NOT_MATCHED;
	}

	/**
	 * Getter for the extension which is used by this draft.
	 *
	 * @return the extension which is used or null, if handshake is not yet done
	 */
	public IExtension getExtension() {
		return this.negotiatedExtension;
	}

	/**
	 * Getter for all available extensions for this draft.
	 *
	 * @return the extensions which are enabled for this draft
	 */
	public List<IExtension> getKnownExtensions() {
		return this.knownExtensions;
	}

	/**
	 * Getter for the protocol which is used by this draft.
	 *
	 * @return the protocol which is used or null, if handshake is not yet done or
	 *         no valid protocols
	 * @since 1.3.7
	 */
	public IProtocol getProtocol() {
		return this.protocol;
	}

	/**
	 * Getter for the maximum allowed payload size which is used by this draft.
	 *
	 * @return the size, which is allowed for the payload
	 * @since 1.4.0
	 */
	public int getMaxFrameSize() {
		return this.maxFrameSize;
	}

	/**
	 * Getter for all available protocols for this draft.
	 *
	 * @return the protocols which are enabled for this draft
	 * @since 1.3.7
	 */
	public List<IProtocol> getKnownProtocols() {
		return this.knownProtocols;
	}

	@Override
	public ClientHandshakeBuilder postProcessHandshakeRequestAsClient(ClientHandshakeBuilder request) {
		request.put(UPGRADE, "websocket");
		request.put(CONNECTION, UPGRADE); // to respond to a Connection keep alives
		byte[] random = new byte[16];
		this.reuseableRandom.nextBytes(random);
		request.put(SEC_WEB_SOCKET_KEY, Base64.encodeBytes(random));
		request.put("Sec-WebSocket-Version", "13");// overwriting the previous
		StringBuilder requestedExtensions = new StringBuilder();
		for (IExtension knownExtension : this.knownExtensions) {
			if (knownExtension.getProvidedExtensionAsClient() != null
					&& knownExtension.getProvidedExtensionAsClient().length() != 0) {
				if (requestedExtensions.length() > 0) {
					requestedExtensions.append(", ");
				}
				requestedExtensions.append(knownExtension.getProvidedExtensionAsClient());
			}
		}
		if (requestedExtensions.length() != 0) {
			request.put(SEC_WEB_SOCKET_EXTENSIONS, requestedExtensions.toString());
		}
		StringBuilder requestedProtocols = new StringBuilder();
		for (IProtocol knownProtocol : this.knownProtocols) {
			if (knownProtocol.getProvidedProtocol().length() != 0) {
				if (requestedProtocols.length() > 0) {
					requestedProtocols.append(", ");
				}
				requestedProtocols.append(knownProtocol.getProvidedProtocol());
			}
		}
		if (requestedProtocols.length() != 0) {
			request.put(SEC_WEB_SOCKET_PROTOCOL, requestedProtocols.toString());
		}
		return request;
	}

	@Override
	public HandshakeBuilder postProcessHandshakeResponseAsServer(ClientHandshake request,
			ServerHandshakeBuilder response) throws InvalidHandshakeException {
		response.put(UPGRADE, "websocket");
		response.put(CONNECTION, request.getFieldValue(CONNECTION)); // to respond to a Connection keep alives
		String seckey = request.getFieldValue(SEC_WEB_SOCKET_KEY);
		if (seckey == null || "".equals(seckey)) {
			throw new InvalidHandshakeException("missing Sec-WebSocket-Key");
		}
		response.put(SEC_WEB_SOCKET_ACCEPT, this.generateFinalKey(seckey));
		if (this.getExtension().getProvidedExtensionAsServer().length() != 0) {
			response.put(SEC_WEB_SOCKET_EXTENSIONS, this.getExtension().getProvidedExtensionAsServer());
		}
		if (this.getProtocol() != null && this.getProtocol().getProvidedProtocol().length() != 0) {
			response.put(SEC_WEB_SOCKET_PROTOCOL, this.getProtocol().getProvidedProtocol());
		}
		response.setHttpStatusMessage("Web Socket Protocol Handshake");
		response.put("Server", "TooTallNate Java-WebSocket");
		response.put("Date", this.getServerTime());
		return response;
	}

	@Override
	public Draft copyInstance() {
		ArrayList<IExtension> newExtensions = new ArrayList<>();
		for (IExtension knownExtension : this.getKnownExtensions()) {
			newExtensions.add(knownExtension.copyInstance());
		}
		ArrayList<IProtocol> newProtocols = new ArrayList<>();
		for (IProtocol knownProtocol : this.getKnownProtocols()) {
			newProtocols.add(knownProtocol.copyInstance());
		}
		return new MyDraft6455(newExtensions, newProtocols, this.maxFrameSize);
	}

	@Override
	public ByteBuffer createBinaryFrame(Framedata framedata) {
		this.getExtension().encodeFrame(framedata);
		if (this.log.isTraceEnabled()) {
			this.log.trace("afterEnconding({}): {}", framedata.getPayloadData().remaining(),
					(framedata.getPayloadData().remaining() > 1000 ? "too big to display"
							: new String(framedata.getPayloadData().array())));
		}
		return this.createByteBufferFromFramedata(framedata);
	}

	private ByteBuffer createByteBufferFromFramedata(Framedata framedata) {
		ByteBuffer mes = framedata.getPayloadData();
		boolean mask = role == Role.CLIENT;
		int sizebytes = this.getSizeBytes(mes);
		final var buf = ByteBuffer
				.allocate(1 + (sizebytes > 1 ? sizebytes + 1 : sizebytes) + (mask ? 4 : 0) + mes.remaining());
		byte optcode = this.fromOpcode(framedata.getOpcode());
		byte one = (byte) (framedata.isFin() ? -128 : 0);
		one |= optcode;
		if (framedata.isRSV1()) {
			one |= this.getRsvByte(1);
		}
		if (framedata.isRSV2()) {
			one |= this.getRsvByte(2);
		}
		if (framedata.isRSV3()) {
			one |= this.getRsvByte(3);
		}
		buf.put(one);
		byte[] payloadlengthbytes = this.toByteArray(mes.remaining(), sizebytes);
		assert (payloadlengthbytes.length == sizebytes);

		if (sizebytes == 1) {
			buf.put((byte) (payloadlengthbytes[0] | this.getMaskByte(mask)));
		} else if (sizebytes == 2) {
			buf.put((byte) ((byte) 126 | this.getMaskByte(mask)));
			buf.put(payloadlengthbytes);
		} else if (sizebytes == 8) {
			buf.put((byte) ((byte) 127 | this.getMaskByte(mask)));
			buf.put(payloadlengthbytes);
		} else {
			throw new IllegalStateException("Size representation not supported/specified");
		}
		if (mask) {
			ByteBuffer maskkey = ByteBuffer.allocate(4);
			maskkey.putInt(this.reuseableRandom.nextInt());
			buf.put(maskkey.array());
			for (int i = 0; mes.hasRemaining(); i++) {
				buf.put((byte) (mes.get() ^ maskkey.get(i % 4)));
			}
		} else {
			buf.put(mes);
			// Reset the position of the bytebuffer e.g. for additional use
			mes.flip();
		}
		assert (buf.remaining() == 0) : buf.remaining();
		buf.flip();
		return buf;
	}

	private Either<Integer, Framedata> translateSingleFrame(ByteBuffer buffer) throws InvalidDataException {
		if (buffer == null) {
			throw new IllegalArgumentException();
		}
		int maxpacketsize = buffer.remaining();
		int realpacketsize = 2;
		if (!this.isFrameSizeValid(maxpacketsize, realpacketsize)) {
			return Either.left(realpacketsize);
		}
		byte b1 = buffer.get(/* 0 */);
		final var fin = b1 >> 8 != 0;
		final var rsv1 = (b1 & 0x40) != 0;
		final var rsv2 = (b1 & 0x20) != 0;
		final var rsv3 = (b1 & 0x10) != 0;
		byte b2 = buffer.get(/* 1 */);
		boolean mask = (b2 & -128) != 0;
		int payloadlength = (byte) (b2 & ~(byte) 128);
		Opcode optcode = this.toOpcode((byte) (b1 & 15));

		if (!(payloadlength >= 0 && payloadlength <= 125)) {
			Either<Integer, TranslatedPayloadMetaData> payloadData = this.translateSingleFramePayloadLength(buffer,
					optcode, payloadlength, maxpacketsize, realpacketsize);
			if (payloadData.left != null) {
				return Either.left(payloadData.left);
			}
			payloadlength = payloadData.right.getPayloadLength();
			realpacketsize = payloadData.right.getRealPackageSize();
		}
		this.translateSingleFrameCheckLengthLimit(payloadlength);
		realpacketsize += (mask ? 4 : 0);
		realpacketsize += payloadlength;

		if (!this.isFrameSizeValid(maxpacketsize, realpacketsize)) {
			return Either.left(realpacketsize);
		}

		ByteBuffer payload = ByteBuffer.allocate(checkAlloc(payloadlength));
		if (mask) {
			byte[] maskskey = new byte[4];
			buffer.get(maskskey);
			for (int i = 0; i < payloadlength; i++) {
				payload.put((byte) (buffer.get(/* payloadstart + i */) ^ maskskey[i % 4]));
			}
		} else {
			payload.put(buffer.array(), buffer.position(), payload.limit());
			buffer.position(buffer.position() + payload.limit());
		}

		FramedataImpl1 frame = FramedataImpl1.get(optcode);
		frame.setFin(fin);
		frame.setRSV1(rsv1);
		frame.setRSV2(rsv2);
		frame.setRSV3(rsv3);
		payload.flip();
		frame.setPayload(payload);
		if (frame.getOpcode() != Opcode.CONTINUOUS) {
			// Prioritize the negotiated extension
			if (frame.isRSV1() || frame.isRSV2() || frame.isRSV3()) {
				this.currentDecodingExtension = this.getExtension();
			} else {
				// No encoded message, so we can use the default one
				this.currentDecodingExtension = this.defaultExtension;
			}
		}
		if (this.currentDecodingExtension == null) {
			this.currentDecodingExtension = this.defaultExtension;
		}
		this.currentDecodingExtension.isFrameValid(frame);
		this.currentDecodingExtension.decodeFrame(frame);
		if (this.log.isTraceEnabled()) {
			this.log.trace("afterDecoding({}): {}", frame.getPayloadData().remaining(),
					(frame.getPayloadData().remaining() > 1000 ? "too big to display"
							: new String(frame.getPayloadData().array())));
		}
		frame.isValid();
		return Either.right(frame);
	}

	/**
	 * Translate the buffer depending when it has an extended payload length (126 or
	 * 127).
	 *
	 * @param buffer            the buffer to read from
	 * @param optcode           the decoded optcode
	 * @param oldPayloadlength  the old payload length
	 * @param maxpacketsize     the max packet size allowed
	 * @param oldRealpacketsize the real packet size
	 * @return the new payload data containing new payload length and new packet
	 *         size
	 * @throws InvalidFrameException  thrown if a control frame has an invalid
	 *                                length
	 * @throws IncompleteException    if the maxpacketsize is smaller than the
	 *                                realpackagesize
	 * @throws LimitExceededException if the payload length is to big
	 */
	private Either<Integer, TranslatedPayloadMetaData> translateSingleFramePayloadLength(ByteBuffer buffer,
			Opcode optcode, int oldPayloadlength, int maxpacketsize, int oldRealpacketsize)
			throws InvalidFrameException, LimitExceededException {
		int payloadlength = oldPayloadlength;
		int realpacketsize = oldRealpacketsize;
		if (optcode == Opcode.PING || optcode == Opcode.PONG || optcode == Opcode.CLOSING) {
			this.log.trace("Invalid frame: more than 125 octets");
			throw new InvalidFrameException("more than 125 octets");
		}
		if (payloadlength == 126) {
			realpacketsize += 2; // additional length bytes

			if (!this.isFrameSizeValid(maxpacketsize, realpacketsize)) {
				return Either.left(realpacketsize);
			}
			byte[] sizebytes = new byte[3];
			sizebytes[1] = buffer.get(/* 1 + 1 */);
			sizebytes[2] = buffer.get(/* 1 + 2 */);
			payloadlength = new BigInteger(sizebytes).intValue();
		} else {
			realpacketsize += 8; // additional length bytes

			if (!this.isFrameSizeValid(maxpacketsize, realpacketsize)) {
				return Either.left(realpacketsize);
			}
			byte[] bytes = new byte[8];
			for (int i = 0; i < 8; i++) {
				bytes[i] = buffer.get(/* 1 + i */);
			}
			long length = new BigInteger(bytes).longValue();
			this.translateSingleFrameCheckLengthLimit(length);
			payloadlength = (int) length;
		}
		return Either.right(new TranslatedPayloadMetaData(payloadlength, realpacketsize));
	}

	/**
	 * Check if the frame size exceeds the allowed limit.
	 *
	 * @param length the current payload length
	 * @throws LimitExceededException if the payload length is to big
	 */
	private void translateSingleFrameCheckLengthLimit(long length) throws LimitExceededException {
		if (length > Integer.MAX_VALUE) {
			this.log.trace("Limit exedeed: Payloadsize is to big...");
			throw new LimitExceededException("Payloadsize is to big...");
		}
		if (length > this.maxFrameSize) {
			this.log.trace("Payload limit reached. Allowed: {} Current: {}", this.maxFrameSize, length);
			throw new LimitExceededException("Payload limit reached.", this.maxFrameSize);
		}
		if (length < 0) {
			this.log.trace("Limit underflow: Payloadsize is to little...");
			throw new LimitExceededException("Payloadsize is to little...");
		}
	}

	private boolean isFrameSizeValid(int maxpacketsize, int realpacketsize) {
		if (maxpacketsize < realpacketsize) {
			this.log.trace("TranslateSingleFrameCheckPacketSize: maxpacketsize < realpacketsize");
			return false;
		}
		return true;
	}

	/**
	 * Get a byte that can set RSV bits when OR(|)'d. 0 1 2 3 4 5 6 7
	 * +-+-+-+-+-------+ |F|R|R|R| opcode| |I|S|S|S| (4) | |N|V|V|V| | | |1|2|3| |
	 *
	 * @param rsv Can only be {0, 1, 2, 3}
	 * @return byte that represents which RSV bit is set.
	 */
	private byte getRsvByte(int rsv) {
		switch (rsv) {
		case 1: // 0100 0000
			return 0x40;
		case 2: // 0010 0000
			return 0x20;
		case 3: // 0001 0000
			return 0x10;
		default:
			return 0;
		}
	}

	/**
	 * Get the mask byte if existing.
	 *
	 * @param mask is mask active or not
	 * @return -128 for true, 0 for false
	 */
	private byte getMaskByte(boolean mask) {
		return mask ? (byte) -128 : 0;
	}

	/**
	 * Get the size bytes for the byte buffer.
	 *
	 * @param mes the current buffer
	 * @return the size bytes
	 */
	private int getSizeBytes(ByteBuffer mes) {
		if (mes.remaining() <= 125) {
			return 1;
		} else if (mes.remaining() <= 65535) {
			return 2;
		}
		return 8;
	}

	@Override
	public List<Framedata> translateFrame(ByteBuffer buffer) throws InvalidDataException {
		while (true) {
			List<Framedata> frames = new LinkedList<>();
			Either<Integer, Framedata> cur;
			if (this.incompleteframe != null) {
				// complete an incomplete frame
				buffer.mark();
				int availableNextByteCount = buffer.remaining();// The number of bytes received
				int expectedNextByteCount = this.incompleteframe.remaining();// The number of bytes to complete
				// the
				// incomplete frame

				if (expectedNextByteCount > availableNextByteCount) {
					// did not receive enough bytes to complete the frame
					this.incompleteframe.put(buffer.array(), buffer.position(), availableNextByteCount);
					buffer.position(buffer.position() + availableNextByteCount);
					return Collections.emptyList();
				}
				this.incompleteframe.put(buffer.array(), buffer.position(), expectedNextByteCount);
				buffer.position(buffer.position() + expectedNextByteCount);
				cur = this.translateSingleFrame((ByteBuffer) this.incompleteframe.duplicate().position(0));

				if (cur.left != null) {
					// extending as much as suggested
					ByteBuffer extendedframe = ByteBuffer.allocate(checkAlloc(cur.left));
					assert (extendedframe.limit() > this.incompleteframe.limit());
					this.incompleteframe.rewind();
					extendedframe.put(this.incompleteframe);
					this.incompleteframe = extendedframe;
					continue;
				}
				frames.add(cur.right);
				this.incompleteframe = null;
			}

			// Read as much as possible full frames
			while (buffer.hasRemaining()) {
				buffer.mark();
				cur = this.translateSingleFrame(buffer);
				if (cur.left != null) {
					// remember the incomplete data
					buffer.reset();
					this.incompleteframe = ByteBuffer.allocate(checkAlloc(cur.left));
					this.incompleteframe.put(buffer);
					break;
				}
				frames.add(cur.right);
			}
			return frames;
		}
	}

	@Override
	public List<Framedata> createFrames(ByteBuffer binary, boolean mask) {
		BinaryFrame curframe = new BinaryFrame();
		curframe.setPayload(binary);
		curframe.setTransferemasked(mask);
		try {
			curframe.isValid();
		} catch (InvalidDataException e) {
			throw new NotSendableException(e);
		}
		return Collections.singletonList((Framedata) curframe);
	}

	@Override
	public List<Framedata> createFrames(String text, boolean mask) {
		TextFrame curframe = new TextFrame();
		curframe.setPayload(ByteBuffer.wrap(Charsetfunctions.utf8Bytes(text)));
		curframe.setTransferemasked(mask);
		try {
			curframe.isValid();
		} catch (InvalidDataException e) {
			throw new NotSendableException(e);
		}
		return Collections.singletonList((Framedata) curframe);
	}

	@Override
	public void reset() {
		this.incompleteframe = null;
		if (this.negotiatedExtension != null) {
			this.negotiatedExtension.reset();
		}
		this.negotiatedExtension = new DefaultExtension();
		this.protocol = null;
	}

	/**
	 * Generate a date for for the date-header.
	 *
	 * @return the server time
	 */
	private String getServerTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(calendar.getTime());
	}

	/**
	 * Generate a final key from a input string.
	 *
	 * @param in the input string
	 * @return a final key
	 */
	private String generateFinalKey(String in) {
		String seckey = in.trim();
		String acc = seckey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest sh1;
		try {
			sh1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
		return Base64.encodeBytes(sh1.digest(acc.getBytes()));
	}

	private byte[] toByteArray(long val, int bytecount) {
		byte[] buffer = new byte[bytecount];
		int highest = 8 * bytecount - 8;
		for (int i = 0; i < bytecount; i++) {
			buffer[i] = (byte) (val >>> (highest - 8 * i));
		}
		return buffer;
	}

	private byte fromOpcode(Opcode opcode) {
		if (opcode == Opcode.CONTINUOUS) {
			return 0;
		} else if (opcode == Opcode.TEXT) {
			return 1;
		} else if (opcode == Opcode.BINARY) {
			return 2;
		} else if (opcode == Opcode.CLOSING) {
			return 8;
		} else if (opcode == Opcode.PING) {
			return 9;
		} else if (opcode == Opcode.PONG) {
			return 10;
		}
		throw new IllegalArgumentException("Don't know how to handle " + opcode.toString());
	}

	private Opcode toOpcode(byte opcode) throws InvalidFrameException {
		switch (opcode) {
		case 0:
			return Opcode.CONTINUOUS;
		case 1:
			return Opcode.TEXT;
		case 2:
			return Opcode.BINARY;
		// 3-7 are not yet defined
		case 8:
			return Opcode.CLOSING;
		case 9:
			return Opcode.PING;
		case 10:
			return Opcode.PONG;
		// 11-15 are not yet defined
		default:
			throw new InvalidFrameException("Unknown opcode " + (short) opcode);
		}
	}

	@Override
	public void processFrame(WebSocketImpl webSocketImpl, Framedata frame) throws InvalidDataException {
		Opcode curop = frame.getOpcode();
		if (curop == Opcode.CLOSING) {
			this.processFrameClosing(webSocketImpl, frame);
		} else if (curop == Opcode.PING) {
			webSocketImpl.getWebSocketListener().onWebsocketPing(webSocketImpl, frame);
		} else if (curop == Opcode.PONG) {
			webSocketImpl.updateLastPong();
			webSocketImpl.getWebSocketListener().onWebsocketPong(webSocketImpl, frame);
		} else if (!frame.isFin() || curop == Opcode.CONTINUOUS) {
			this.processFrameContinuousAndNonFin(webSocketImpl, frame, curop);
		} else if (this.currentContinuousFrame != null) {
			this.log.error("Protocol error: Continuous frame sequence not completed.");
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR, "Continuous frame sequence not completed.");
		} else if (curop == Opcode.TEXT) {
			this.processFrameText(webSocketImpl, frame);
		} else if (curop == Opcode.BINARY) {
			this.processFrameBinary(webSocketImpl, frame);
		} else {
			this.log.error("non control or continious frame expected");
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR, "non control or continious frame expected");
		}
	}

	/**
	 * Process the frame if it is a continuous frame or the fin bit is not set.
	 *
	 * @param webSocketImpl the websocket implementation to use
	 * @param frame         the current frame
	 * @param curop         the current Opcode
	 * @throws InvalidDataException if there is a protocol error
	 */
	private void processFrameContinuousAndNonFin(WebSocketImpl webSocketImpl, Framedata frame, Opcode curop)
			throws InvalidDataException {
		if (curop != Opcode.CONTINUOUS) {
			this.processFrameIsNotFin(frame);
		} else if (frame.isFin()) {
			this.processFrameIsFin(webSocketImpl, frame);
		} else if (this.currentContinuousFrame == null) {
			this.log.error("Protocol error: Continuous frame sequence was not started.");
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR, "Continuous frame sequence was not started.");
		}
		// Check if the whole payload is valid utf8, when the opcode indicates a text
		if (curop == Opcode.TEXT && !Charsetfunctions.isValidUTF8(frame.getPayloadData())) {
			this.log.error("Protocol error: Payload is not UTF8");
			throw new InvalidDataException(CloseFrame.NO_UTF8);
		}
		// Checking if the current continuous frame contains a correct payload with the
		// other frames combined
		if (curop == Opcode.CONTINUOUS && this.currentContinuousFrame != null) {
			this.addToBufferList(frame.getPayloadData());
		}
	}

	/**
	 * Process the frame if it is a binary frame.
	 *
	 * @param webSocketImpl the websocket impl
	 * @param frame         the frame
	 */
	private void processFrameBinary(WebSocketImpl webSocketImpl, Framedata frame) {
		try {
			webSocketImpl.getWebSocketListener().onWebsocketMessage(webSocketImpl, frame.getPayloadData());
		} catch (RuntimeException e) {
			this.logRuntimeException(webSocketImpl, e);
		}
	}

	/**
	 * Log the runtime exception to the specific WebSocketImpl.
	 *
	 * @param webSocketImpl the implementation of the websocket
	 * @param e             the runtime exception
	 */
	private void logRuntimeException(WebSocketImpl webSocketImpl, RuntimeException e) {
		this.log.error("Runtime exception during onWebsocketMessage", e);
		webSocketImpl.getWebSocketListener().onWebsocketError(webSocketImpl, e);
	}

	/**
	 * Process the frame if it is a text frame.
	 *
	 * @param webSocketImpl the websocket impl
	 * @param frame         the frame
	 */
	private void processFrameText(WebSocketImpl webSocketImpl, Framedata frame) throws InvalidDataException {
		try {
			webSocketImpl.getWebSocketListener().onWebsocketMessage(webSocketImpl,
					Charsetfunctions.stringUtf8(frame.getPayloadData()));
		} catch (RuntimeException e) {
			this.logRuntimeException(webSocketImpl, e);
		}
	}

	/**
	 * Process the frame if it is the last frame.
	 *
	 * @param webSocketImpl the websocket impl
	 * @param frame         the frame
	 * @throws InvalidDataException if there is a protocol error
	 */
	private void processFrameIsFin(WebSocketImpl webSocketImpl, Framedata frame) throws InvalidDataException {
		if (this.currentContinuousFrame == null) {
			this.log.trace("Protocol error: Previous continuous frame sequence not completed.");
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR, "Continuous frame sequence was not started.");
		}
		this.addToBufferList(frame.getPayloadData());
		this.checkBufferLimit();
		if (this.currentContinuousFrame.getOpcode() == Opcode.TEXT) {
			((FramedataImpl1) this.currentContinuousFrame).setPayload(this.getPayloadFromByteBufferList());
			((FramedataImpl1) this.currentContinuousFrame).isValid();
			try {
				webSocketImpl.getWebSocketListener().onWebsocketMessage(webSocketImpl,
						Charsetfunctions.stringUtf8(this.currentContinuousFrame.getPayloadData()));
			} catch (RuntimeException e) {
				this.logRuntimeException(webSocketImpl, e);
			}
		} else if (this.currentContinuousFrame.getOpcode() == Opcode.BINARY) {
			((FramedataImpl1) this.currentContinuousFrame).setPayload(this.getPayloadFromByteBufferList());
			((FramedataImpl1) this.currentContinuousFrame).isValid();
			try {
				webSocketImpl.getWebSocketListener().onWebsocketMessage(webSocketImpl,
						this.currentContinuousFrame.getPayloadData());
			} catch (RuntimeException e) {
				this.logRuntimeException(webSocketImpl, e);
			}
		}
		this.currentContinuousFrame = null;
		this.clearBufferList();
	}

	/**
	 * Process the frame if it is not the last frame.
	 *
	 * @param frame the frame
	 * @throws InvalidDataException if there is a protocol error
	 */
	private void processFrameIsNotFin(Framedata frame) throws InvalidDataException {
		if (this.currentContinuousFrame != null) {
			this.log.trace("Protocol error: Previous continuous frame sequence not completed.");
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR,
					"Previous continuous frame sequence not completed.");
		}
		this.currentContinuousFrame = frame;
		this.addToBufferList(frame.getPayloadData());
		this.checkBufferLimit();
	}

	/**
	 * Process the frame if it is a closing frame.
	 *
	 * @param webSocketImpl the websocket impl
	 * @param frame         the frame
	 */
	private void processFrameClosing(WebSocketImpl webSocketImpl, Framedata frame) {
		int code = CloseFrame.NOCODE;
		String reason = "";
		if (frame instanceof CloseFrame) {
			CloseFrame cf = (CloseFrame) frame;
			code = cf.getCloseCode();
			reason = cf.getMessage();
		}
		if (webSocketImpl.getReadyState() == ReadyState.CLOSING) {
			// complete the close handshake by disconnecting
			webSocketImpl.closeConnection(code, reason, true);
		} else {
			// echo close handshake
			if (this.getCloseHandshakeType() == CloseHandshakeType.TWOWAY) {
				webSocketImpl.close(code, reason, true);
			} else {
				webSocketImpl.flushAndClose(code, reason, false);
			}
		}
	}

	/**
	 * Clear the current bytebuffer list.
	 */
	private void clearBufferList() {
		synchronized (this.byteBufferList) {
			this.byteBufferList.clear();
		}
	}

	/**
	 * Add a payload to the current bytebuffer list.
	 *
	 * @param payloadData the new payload
	 */
	private void addToBufferList(ByteBuffer payloadData) {
		synchronized (this.byteBufferList) {
			this.byteBufferList.add(payloadData);
		}
	}

	/**
	 * Check the current size of the buffer and throw an exception if the size is
	 * bigger than the max allowed frame size.
	 *
	 * @throws LimitExceededException if the current size is bigger than the allowed
	 *                                size
	 */
	private void checkBufferLimit() throws LimitExceededException {
		long totalSize = this.getByteBufferListSize();
		if (totalSize > this.maxFrameSize) {
			this.clearBufferList();
			this.log.trace("Payload limit reached. Allowed: {} Current: {}", this.maxFrameSize, totalSize);
			throw new LimitExceededException(this.maxFrameSize);
		}
	}

	@Override
	public CloseHandshakeType getCloseHandshakeType() {
		return CloseHandshakeType.TWOWAY;
	}

	@Override
	public String toString() {
		String result = super.toString();
		if (this.getExtension() != null) {
			result += " extension: " + this.getExtension().toString();
		}
		if (this.getProtocol() != null) {
			result += " protocol: " + this.getProtocol().toString();
		}
		result += " max frame size: " + this.maxFrameSize;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		MyDraft6455 that = (MyDraft6455) o;

		if (this.maxFrameSize != that.getMaxFrameSize()) {
			return false;
		}
		if (this.negotiatedExtension != null ? !this.negotiatedExtension.equals(that.getExtension())
				: that.getExtension() != null) {
			return false;
		}
		return this.protocol != null ? this.protocol.equals(that.getProtocol()) : that.getProtocol() == null;
	}

	@Override
	public int hashCode() {
		int result = this.negotiatedExtension != null ? this.negotiatedExtension.hashCode() : 0;
		result = 31 * result + (this.protocol != null ? this.protocol.hashCode() : 0);
		result = 31 * result + (this.maxFrameSize ^ (this.maxFrameSize >>> 32));
		return result;
	}

	/**
	 * Method to generate a full bytebuffer out of all the fragmented frame payload.
	 *
	 * @return a bytebuffer containing all the data
	 * @throws LimitExceededException will be thrown when the totalSize is bigger
	 *                                then Integer.MAX_VALUE due to not being able
	 *                                to allocate more
	 */
	private ByteBuffer getPayloadFromByteBufferList() throws LimitExceededException {
		long totalSize = 0;
		ByteBuffer resultingByteBuffer;
		synchronized (this.byteBufferList) {
			for (ByteBuffer buffer : this.byteBufferList) {
				totalSize += buffer.limit();
			}
			this.checkBufferLimit();
			resultingByteBuffer = ByteBuffer.allocate((int) totalSize);
			for (ByteBuffer buffer : this.byteBufferList) {
				resultingByteBuffer.put(buffer);
			}
		}
		resultingByteBuffer.flip();
		return resultingByteBuffer;
	}

	/**
	 * Get the current size of the resulting bytebuffer in the bytebuffer list.
	 *
	 * @return the size as long (to not get an integer overflow)
	 */
	private long getByteBufferListSize() {
		long totalSize = 0;
		synchronized (this.byteBufferList) {
			for (ByteBuffer buffer : this.byteBufferList) {
				totalSize += buffer.limit();
			}
		}
		return totalSize;
	}

	private class TranslatedPayloadMetaData {

		private int payloadLength;
		private int realPackageSize;

		private int getPayloadLength() {
			return this.payloadLength;
		}

		private int getRealPackageSize() {
			return this.realPackageSize;
		}

		TranslatedPayloadMetaData(int newPayloadLength, int newRealPackageSize) {
			this.payloadLength = newPayloadLength;
			this.realPackageSize = newRealPackageSize;
		}
	}

}
