
package io.openems.edge.controller.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Constants;

import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.common.handler.ComponentRequestHandler;

@ExtendWith(MockitoExtension.class)
class ComponentRequestHandlerTest {

	@InjectMocks
	private ComponentRequestHandler sut;

	@Mock
	private ComponentJsonApi componentJsonApi;

	@Test
	void shouldBindJsonApi() throws Exception {
		// Arrange
		when(this.componentJsonApi.id()).thenReturn("ess0");

		var ref = serviceRef(1L);

		// Act
		this.sut.bindJsonApi(this.componentJsonApi, ref);

		// Assert
		var jsonApis = this.getJsonApis();

		assertEquals(1, jsonApis.size());
		assertTrue(jsonApis.containsKey("ess0"));
		verify(this.componentJsonApi).buildJsonApiRoutes(any(JsonApiBuilder.class));
	}

	@Test
	void shouldUpdateComponentIdOnUpdateJsonApi() throws Exception {
		// Arrange
		var componentId = new AtomicReference<>("ess0");
		when(this.componentJsonApi.id()).thenAnswer(invocation -> componentId.get());

		var ref = serviceRef(1L);
		this.sut.bindJsonApi(this.componentJsonApi, ref);

		// Act
		componentId.set("ess1");
		this.sut.updateJsonApi(this.componentJsonApi, ref);

		// Assert
		var jsonApis = this.getJsonApis();

		assertEquals(1, jsonApis.size());
		assertFalse(jsonApis.containsKey("ess0"));
		assertTrue(jsonApis.containsKey("ess1"));
	}

	@Test
	void shouldOnlyUpdateMatchingServiceId() throws Exception {
		// Arrange
		var componentId = new AtomicReference<>("ess0");
		when(this.componentJsonApi.id()).thenAnswer(invocation -> componentId.get());

		var otherApi = mock(ComponentJsonApi.class);
		when(otherApi.id()).thenReturn("ess1");

		var ref = serviceRef(1L);
		this.sut.bindJsonApi(this.componentJsonApi, ref);
		this.sut.bindJsonApi(otherApi, serviceRef(2L));

		// Act
		componentId.set("ess2");
		this.sut.updateJsonApi(this.componentJsonApi, ref);

		// Assert
		var jsonApis = this.getJsonApis();

		assertEquals(2, jsonApis.size());
		assertFalse(jsonApis.containsKey("ess0"));
		assertTrue(jsonApis.containsKey("ess1"));
		assertTrue(jsonApis.containsKey("ess2"));
	}

	@Test
	void shouldUnbindJsonApi() throws Exception {
		// Arrange
		when(this.componentJsonApi.id()).thenReturn("ess0");

		var ref = serviceRef(1L);
		this.sut.bindJsonApi(this.componentJsonApi, ref);

		// Act
		this.sut.unbindJsonApi(this.componentJsonApi, ref);

		// Assert
		assertTrue(this.getJsonApis().isEmpty());
	}

	@Test
	void shouldBindExtensionToRegisteredComponent() {
		// Arrange
		when(this.componentJsonApi.id()).thenReturn("ess0");

		@SuppressWarnings("unchecked")
		ComponentJsonApiExtension<ComponentJsonApi> extension = mock(ComponentJsonApiExtension.class);

		doReturn(ComponentJsonApi.class).when(extension).extendClass();

		this.sut.bindJsonApi(this.componentJsonApi, serviceRef(1L));

		// Act
		this.sut.bindExtension(extension);

		// Assert
		verify(extension).buildJsonApiRoutes(eq(this.componentJsonApi), any(JsonApiBuilder.class));
	}

	private static Map<String, Object> serviceRef(long serviceId) {
		return Map.of(Constants.SERVICE_ID, serviceId);
	}

	@SuppressWarnings("unchecked")
	private Map<String, ?> getJsonApis() throws Exception {
		var field = ComponentRequestHandler.class.getDeclaredField("jsonApis");
		field.setAccessible(true);

		return (Map<String, ?>) field.get(this.sut);
	}
}
