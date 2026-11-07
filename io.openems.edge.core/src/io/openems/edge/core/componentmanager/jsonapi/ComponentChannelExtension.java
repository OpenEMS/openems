package io.openems.edge.core.componentmanager.jsonapi;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;

import io.openems.common.channel.ChannelCategory;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent;
import io.openems.common.jsonrpc.request.GetStateChannelsOfComponent;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.StateChannelDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannel;
import io.openems.edge.core.componentmanager.jsonrpc.GetChannelCount;
import io.openems.edge.core.componentmanager.jsonrpc.GetDigitalInputChannelsOfComponents;
import io.openems.edge.io.api.DigitalInput;

@Component
public class ComponentChannelExtension implements ComponentJsonApiExtension<ComponentManagerImpl> {

	@Override
	public Class<? extends ComponentManagerImpl> extendClass() {
		return ComponentManagerImpl.class;
	}

	@Override
	public void buildJsonApiRoutes(ComponentManagerImpl componentManager, JsonApiBuilder builder) {
		builder.handleRequest(new GetStateChannelsOfComponent(), //
				endpoint -> endpoint.setDescription("""
						Handles a GetStateChannelsOfComponent.
						"""), //
				call -> {
					final var user = call.get(EdgeKeys.USER_KEY);
					final var lang = user.getLanguage();

					final var channels = componentManager.getPossiblyDisabledComponent(call.getRequest().componentId())
							.channels().stream() //
							.filter(t -> t.channelDoc().getChannelCategory() == ChannelCategory.STATE) //
							.map(channel -> toChannelRecord(channel, lang)) //
							.toList();

					return new GetStateChannelsOfComponent.Response(channels);
				});

		builder.handleRequest(new GetChannelsOfComponent(), //
				endpoint -> endpoint.setDescription("""
						Handles a GetChannelsOfComponent.
						"""), //
				call -> {

					final var user = call.get(EdgeKeys.USER_KEY);
					final var lang = user.getLanguage();

					final OpenemsComponent component;
					if (call.getRequest().requireEnabled()) {
						component = componentManager.getComponent(call.getRequest().componentId());
					} else {
						component = componentManager.getPossiblyDisabledComponent(call.getRequest().componentId());
					}
					final var channels = component.channels().stream() //
							.map(channel -> toChannelRecord(channel, lang)) //
							.toList();
					return new GetChannelsOfComponent.Response(channels);
				});

		builder.handleRequest(new GetChannel(), //
				endpoint -> endpoint.setDescription("""
						Handles a GetChannel.
						"""), //
				call -> {
					final var request = call.getRequest();
					final var channel = componentManager
							.getChannel(new ChannelAddress(request.componentId(), request.channelId()));

					final var user = call.get(EdgeKeys.USER_KEY);
					final var lang = user.getLanguage();
					return new GetChannel.Response(toChannelRecord(channel, lang));
				});

		builder.handleRequest(new GetDigitalInputChannelsOfComponents(), //
				endpoint -> endpoint.setDescription("""
						Handles a GetDigitalInputChannelsOfComponent.
						"""), //
				call -> {

					final var user = call.get(EdgeKeys.USER_KEY);
					final var lang = user.getLanguage();
					final var result = componentManager.getEnabledComponentsOfType(DigitalInput.class).stream() //
							.filter(t -> call.getRequest().componentIds().contains(t.id())) //
							.collect(toMap(OpenemsComponent::id, t -> Arrays.stream(t.digitalInputChannels()) //
									.map(channel -> toChannelRecord(channel, lang)) //
									.toList()));

					return new GetDigitalInputChannelsOfComponents.Response(result);
				});

		builder.handleRequest(new GetChannelCount(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			final var numberOfChannels = componentManager.getAllComponents().stream() //
					.mapToLong(t -> t.channels().size()) //
					.sum();

			return new GetChannelCount.Response(numberOfChannels);
		});

	}

	private static GetChannelsOfComponent.ChannelRecord toChannelRecord(Channel<?> channel, Language language) {
		return new GetChannelsOfComponent.ChannelRecord(//
				channel.channelId().id(), //
				channel.channelDoc().getAccessMode(), //
				channel.channelDoc().getRemotePersistencePriority(), //
				channel.channelDoc().getText(language), //
				channel.channelDoc().getType(), //
				channel.channelDoc().getUnit(), //
				channel.channelDoc().getChannelCategory(), //
				channel.channelDoc() instanceof StateChannelDoc c ? c.getLevel() : null, //
				channel.channelDoc() instanceof EnumDoc c ? Stream.of(c.getOptions()) //
						.map(GetChannelsOfComponent.ChannelRecord.OptionsEnumEntry::from) //
						.toList() : null);
	}

}
