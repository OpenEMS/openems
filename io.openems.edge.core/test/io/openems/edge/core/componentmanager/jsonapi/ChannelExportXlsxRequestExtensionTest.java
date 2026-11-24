package io.openems.edge.core.componentmanager.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsxRequest;

@ExtendWith(MockitoExtension.class)
class ChannelExportXlsxRequestExtensionTest {

	@InjectMocks
	private ChannelExportXlsxRequestExtension sut;

	@Mock
	private ComponentManagerImpl componentManager;

	@Test
	void shouldExtendComponentManagerImpl() {
		assertEquals(ComponentManagerImpl.class, this.sut.extendClass());
	}

	@Test
	void shouldRegisterAllRequests() {
		// Arrange
		var builder = new JsonApiBuilder();

		// Act
		this.sut.buildJsonApiRoutes(this.componentManager, builder);

		// Assert
		var endpoints = builder.getEndpoints();

		assertEquals(1, endpoints.size());
		assertTrue(endpoints.containsKey(ChannelExportXlsxRequest.METHOD));
	}

	@Test
	void shouldReturnChannelExportXlsxResponse() throws Exception {
		// Arrange
		var request = mock(ChannelExportXlsxRequest.class);
		var component = mock(OpenemsComponent.class);

		when(request.getComponentId()).thenReturn("ctrl0");
		when(this.componentManager.getComponent("ctrl0")).thenReturn(component);

		// Act
		var response = this.sut.handleChannelExportXlsxRequest(this.componentManager, request);

		// Assert
		assertNotNull(response);
		verify(this.componentManager).getComponent("ctrl0");
	}

	@Test
	void shouldThrowExceptionWhenComponentDoesNotExist() throws Exception {
		// Arrange
		var request = mock(ChannelExportXlsxRequest.class);

		when(request.getComponentId()).thenReturn("unknownComponent");

		// Act
		var exception = assertThrows(OpenemsError.OpenemsNamedException.class,
				() -> this.sut.handleChannelExportXlsxRequest(this.componentManager, request));

		// Assert
		assertNotNull(exception);
		verify(this.componentManager).getComponent("unknownComponent");
	}
}