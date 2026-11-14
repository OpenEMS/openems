package io.openems.edge.core.componentmanager.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openems.common.function.ThrowingFunction;
import io.openems.common.session.Language;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannel;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelCount;

@ExtendWith(MockitoExtension.class)
class ComponentChannelExtensionTest {

	@InjectMocks
	private ComponentChannelExtension sut;

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

		assertEquals(5, endpoints.size());
		assertTrue(endpoints.containsKey("getStateChannelsOfComponent"));
		assertTrue(endpoints.containsKey("getChannelsOfComponent"));
		assertTrue(endpoints.containsKey("getChannel"));
		assertTrue(endpoints.containsKey("getDigitalInputChannelsOfComponents"));
		assertTrue(endpoints.containsKey("getChannelCount"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldForwardGetChannelRequestToComponentManager() throws Exception {
		// Arrange
		this.sut.buildJsonApiRoutes(this.componentManager, this.builder);

		var handlerCaptor = ArgumentCaptor.forClass(ThrowingFunction.class);

		verify(this.builder).handleRequest(any(GetChannel.class), any(), handlerCaptor.capture());

		var request = new GetChannel.Request("ctrl0", "Enabled");

		var user = mock(User.class);
		when(user.getLanguage()).thenReturn(Language.DEFAULT);

		var call = new Call<GetChannel.Request, GetChannel.Response>(request);
		call.put(EdgeKeys.USER_KEY, user);

		var channel = mock(Channel.class);
		var channelId = mock(ChannelId.class);
		var channelDoc = mock(Doc.class);

		when(channel.channelDoc()).thenReturn(channelDoc);
		when(channel.channelId()).thenReturn(channelId);
		when(this.componentManager.getChannel(any())).thenReturn(channel);

		// Act
		handlerCaptor.getValue().apply(call);

		// Assert
		var addressCaptor = ArgumentCaptor.forClass(ChannelAddress.class);

		verify(this.componentManager).getChannel(addressCaptor.capture());

		assertEquals("ctrl0", addressCaptor.getValue().getComponentId());
		assertEquals("Enabled", addressCaptor.getValue().getChannelId());
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldReturnSumOfAllChannels() throws Exception {
		// Arrange
		this.sut.buildJsonApiRoutes(this.componentManager, this.builder);

		var component1 = mock(OpenemsComponent.class);
		var component2 = mock(OpenemsComponent.class);

		when(component1.channels()).thenReturn(List.of(mock(Channel.class), mock(Channel.class)));

		when(component2.channels()).thenReturn(List.of(mock(Channel.class)));

		when(this.componentManager.getAllComponents()).thenReturn(List.of(component1, component2));

		var handlerCaptor = ArgumentCaptor.forClass(ThrowingFunction.class);

		verify(this.builder).handleRequest(any(GetChannelCount.class), any(), handlerCaptor.capture());

		var call = mock(Call.class);

		// Act
		var response = (GetChannelCount.Response) handlerCaptor.getValue().apply(call);

		// Assert
		assertEquals(3, response.numberOfChannels());

		verify(this.componentManager).getAllComponents();
		verify(component1).channels();
		verify(component2).channels();
	}
}