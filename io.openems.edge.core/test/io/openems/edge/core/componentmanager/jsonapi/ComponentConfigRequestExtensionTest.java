package io.openems.edge.core.componentmanager.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;

@ExtendWith(MockitoExtension.class)
class ComponentConfigRequestExtensionTest {

	@InjectMocks
	private ComponentConfigRequestExtension sut;

	@Mock
	private ComponentManagerImpl componentManager;

	@Test
	void shouldExtendComponentManagerImpl() {
		assertEquals(ComponentManagerImpl.class, this.sut.extendClass());
	}

	@Test
	void shouldRegisterAllRequests() {
		var builder = new JsonApiBuilder();

		this.sut.buildJsonApiRoutes(this.componentManager, builder);

		var endpoints = builder.getEndpoints();

		assertEquals(3, endpoints.size());
		assertTrue(endpoints.containsKey("createComponentConfig"));
		assertTrue(endpoints.containsKey("updateComponentConfig"));
		assertTrue(endpoints.containsKey("deleteComponentConfig"));
	}

}