package io.openems.backend.edgewebsocket;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Optional;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcMessage;

public class WsDataTest {

	private static final String EDGE_ID = "edge0";
	private static final JsonrpcMessage JMSG = new GenericJsonrpcNotification("foo", new JsonObject());

	@Test
	public void test() throws OpenemsException {
		var sut = new WsData(null);
		assertEquals("EdgeWebsocket.WsData [edgeId=UNKNOWN]", sut.toLogString());
		assertThrows(OpenemsNamedException.class, () -> sut.assertEdgeId(JMSG));
		assertThrows(OpenemsNamedException.class, () -> sut.assertEdgeIdWithTimeout(JMSG, 1, MILLISECONDS));

		sut.setEdgeId(EDGE_ID);
		assertEquals("EdgeWebsocket.WsData [edgeId=edge0]", sut.toLogString());
		sut.assertEdgeId(null);
		sut.assertEdgeIdWithTimeout(JMSG, 1, MILLISECONDS);
		assertEquals(Optional.of(EDGE_ID), sut.getEdgeId());
	}

}
