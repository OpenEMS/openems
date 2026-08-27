package io.openems.edge.phoenixcontact.plcnext.common.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;

public class PlcNextGdsDataProviderWriteTest {

	private PlcNextGdsDataProviderImpl dataProvider;

	@BeforeEach
	public void setupBefore() {
		DummyBridgeHttp mockDummyBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		when(mockDummyBridgeHttp.createService(any())).thenReturn(new HttpBridgeTimeServiceImpl(mockDummyBridgeHttp, //
				new DummyBridgeHttpExecutor()));

		PlcNextTokenManager mockTokenManager = Mockito.mock(PlcNextTokenManagerImpl.class);

		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyBridgeHttp, mockTokenManager);
	}

	@Test
	public void testSerializationOfJsonObjectStructure() {
		// prep
		String expectedBody = new StringBuilder("{").append("\"sessionID\":\"1234567890\",")
				.append("\"pathPrefix\":\"pathPrefix\",").append("\"variables\":[{").append("\"path\":\"variable_1\",")
				.append("\"value\":1,").append("\"valueType\":\"Variable\"").append("},{")
				.append("\"path\":\"variable_2\",").append("\"value\":2,").append("\"valueType\":\"Variable\"")
				.append("}]}").toString();
		List<JsonElement> variablesToWrite = new ArrayList<>(2);

		JsonObject var1 = new JsonObject();
		var1.addProperty("path", "variable_1");
		var1.addProperty("value", 1);
		var1.addProperty("valueType", PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());
		variablesToWrite.add(var1);

		JsonObject var2 = new JsonObject();
		var2.addProperty("path", "variable_2");
		var2.addProperty("value", 2);
		var2.addProperty("valueType", PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());
		variablesToWrite.add(var2);

		// test
		String sessionId = "1234567890";
		String requestBody = dataProvider.buildPutBodyForWrite(sessionId, variablesToWrite);

		// check
		assertNotNull(requestBody);
		assertFalse(requestBody.isBlank());
		assertEquals(expectedBody, requestBody);
	}

}
