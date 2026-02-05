package io.openems.edge.phoenixcontact.plcnext.common.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.dummy.DummyBridgeHttp;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceImpl;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManager;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextTokenManagerImpl;

public class PlcNextGdsDataProviderWriteTest {

	private String sessionId = "1234567890";

	private DummyBridgeHttp mockDummyBridgeHttp;
	private PlcNextTokenManager mockTokenManager;

	private PlcNextGdsDataProviderImpl dataProvider;

	@Before
	public void setupBefore() {
		mockDummyBridgeHttp = Mockito.mock(DummyBridgeHttp.class);
		when(mockDummyBridgeHttp.createService(any())).thenReturn(new HttpBridgeTimeServiceImpl(mockDummyBridgeHttp, //
				new DummyBridgeHttpExecutor(), new DummyEndpointFetcher()));

		mockTokenManager = Mockito.mock(PlcNextTokenManagerImpl.class);

		this.dataProvider = new PlcNextGdsDataProviderImpl(mockDummyBridgeHttp, mockTokenManager);
	}

	@Test
	public void testSerializationOfJsonObjectStructure() {
		// prep
		String expectedBody = "{\"sessionID\":\"1234567890\",\"pathPrefix\":\"pathPrefix\",\"variables\":[" //
				+ "{\"path\":\"variable_1\",\"value\":1,\"valueType\":\"Variable\"}," //
				+ "{\"path\":\"variable_2\",\"value\":2,\"valueType\":\"Variable\"}" //
				+ "]}";
		List<JsonElement> variablesToWrite = new ArrayList<JsonElement>(2);

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
		String requestBody = dataProvider.buildPutBodyForWrite(sessionId, variablesToWrite);

		// check
		assertNotNull(requestBody);
		assertFalse(requestBody.isBlank());
		assertEquals(expectedBody, requestBody);
	}

}
