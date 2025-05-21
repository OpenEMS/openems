package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemRestartRequest.Type;

public class ExecuteSystemResponseTest {

	@Test
	public void testParse() throws OpenemsNamedException {
		var sut = ExecuteSystemRestartRequest.from(new GenericJsonrpcRequest("executeSystemRestart", buildJsonObject() //
				.addProperty("type", "soft") //
				.build()));
		assertEquals(Type.SOFT, sut.type);

		sut = ExecuteSystemRestartRequest.from(new GenericJsonrpcRequest("executeSystemRestart", buildJsonObject() //
				.addProperty("type", "HARD") //
				.build()));
		assertEquals(Type.HARD, sut.type);
	}

	@Test(expected = OpenemsException.class)
	public void testParseFailed() throws OpenemsNamedException {
		ExecuteSystemRestartRequest.from(new GenericJsonrpcRequest("executeSystemRestart", buildJsonObject() //
				.addProperty("type", "foo") //
				.build()));
	}
}
