package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

	@Test
	public void testParseFailed() throws OpenemsNamedException {
		assertThrows(OpenemsException.class,
				() -> ExecuteSystemRestartRequest.from(new GenericJsonrpcRequest("executeSystemRestart",
						buildJsonObject() //
								.addProperty("type", "foo") //
								.build())));
	}
}
