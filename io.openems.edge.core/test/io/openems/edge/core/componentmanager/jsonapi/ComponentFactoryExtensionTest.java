package io.openems.edge.core.componentmanager.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;
import io.openems.edge.core.componentmanager.jsonrpc.GetPropertiesOfFactory;

@ExtendWith(MockitoExtension.class)
class ComponentFactoryExtensionTest {

	@InjectMocks
	private ComponentFactoryExtension sut;

	@Mock
	private ComponentManagerImpl componentManager;

	@Mock
	private JsonApiBuilder builder;

	@Test
	void shouldExtendComponentManagerImpl() {
		assertEquals(ComponentManagerImpl.class, this.sut.extendClass());
	}

	@Test
	void shouldRegisterAllRequests() {
		var builder = new JsonApiBuilder();

		this.sut.buildJsonApiRoutes(this.componentManager, builder);

		var endpoints = builder.getEndpoints();

		assertEquals(2, endpoints.size());
		assertTrue(endpoints.containsKey("getAllComponentFactories"));
		assertTrue(endpoints.containsKey("getPropertiesOfFactory"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldReturnPropertiesOfRequestedFactory() throws Exception {
		// Arrange
		this.sut.buildJsonApiRoutes(this.componentManager, this.builder);

		var handlerCaptor = ArgumentCaptor.forClass(ThrowingFunction.class);
		verify(this.builder).handleRequest(any(GetPropertiesOfFactory.class), any(), handlerCaptor.capture());

		var edgeConfig = mock(EdgeConfig.class);
		var factory = mock(EdgeConfig.Factory.class);

		var factoryJson = new JsonObject();
		factoryJson.addProperty("id", "factory0");

		when(this.componentManager.getEdgeConfig()).thenReturn(edgeConfig);
		when(edgeConfig.getFactories()).thenReturn(ImmutableSortedMap.of("factory0", factory));
		when(factory.toJson()).thenReturn(factoryJson);
		when(factory.getProperties()).thenReturn(new EdgeConfig.Factory.Property[0]);

		// Act
		var response = (GetPropertiesOfFactory.Response) handlerCaptor.getValue()
				.apply(new Call<GetPropertiesOfFactory.Request, GetPropertiesOfFactory.Response>(
						new GetPropertiesOfFactory.Request("factory0")));

		// Assert
		assertEquals(factoryJson, response.factory());
		assertTrue(response.properties().isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldThrowExceptionWhenFactoryDoesNotExist() throws Exception {
		// Arrange
		this.sut.buildJsonApiRoutes(this.componentManager, this.builder);

		var handlerCaptor = ArgumentCaptor.forClass(ThrowingFunction.class);
		verify(this.builder).handleRequest(any(GetPropertiesOfFactory.class), any(), handlerCaptor.capture());

		var request = new GetPropertiesOfFactory.Request("unknownFactory");
		var call = new Call<GetPropertiesOfFactory.Request, GetPropertiesOfFactory.Response>(request);

		var edgeConfig = mock(EdgeConfig.class);

		when(this.componentManager.getEdgeConfig()).thenReturn(edgeConfig);
		when(edgeConfig.getFactories()).thenReturn(ImmutableSortedMap.of());

		// Act
		var exception = assertThrows(OpenemsException.class, () -> handlerCaptor.getValue().apply(call));

		// Assert
		assertEquals("Factory with id unknownFactory could not be found.", exception.getMessage());
	}
}