package io.openems.edge.phoenixcontact.plcnext.common.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;

public class PlcNextGdsDataProviderWriteTest {

	private PlcNextGdsDataProviderImpl dataProvider;

	@Before
	public void setupBefore() {
		DummyBridgeHttp mockDummyBridgeHttp = mock(DummyBridgeHttp.class);
		when(mockDummyBridgeHttp.createService(any()))
				.thenReturn(new HttpBridgeTimeServiceImpl(mockDummyBridgeHttp, new DummyBridgeHttpExecutor()));

		PlcNextTokenManager mockTokenManager = mock(PlcNextTokenManagerImpl.class);

		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyBridgeHttp, mockTokenManager);
	}

	@Test
	public void testSerializationOfJsonObjectStructure() {
		// prep
		var var1 = new JsonObject();
		var1.addProperty("path", "variable_1");
		var1.addProperty("value", 1);
		var1.addProperty("valueType", PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());

		var var2 = new JsonObject();
		var2.addProperty("path", "variable_2");
		var2.addProperty("value", 2);
		var2.addProperty("valueType", PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());

		var variablesToWrite = new ArrayList<JsonElement>(2);
		variablesToWrite.add(var1);
		variablesToWrite.add(var2);

		// test
		var sessionId = "1234567890";
		var requestBody = this.dataProvider.buildPutBodyForWrite(sessionId, variablesToWrite);

		// check
		var expectedBody = new StringBuilder("{") //
				.append("\"sessionID\":\"1234567890\",") //
				.append("\"pathPrefix\":\"pathPrefix\",") //
				.append("\"variables\":[{") //
				.append("\"path\":\"variable_1\",") //
				.append("\"value\":1,") //
				.append("\"valueType\":\"Variable\"") //
				.append("},{") //
				.append("\"path\":\"variable_2\",") //
				.append("\"value\":2,") //
				.append("\"valueType\":\"Variable\"") //
				.append("}]}") //
				.toString();

		assertNotNull(requestBody);
		assertFalse(requestBody.isBlank());
		assertEquals(expectedBody, requestBody);
	}

}
