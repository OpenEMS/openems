package io.openems.edge.ess.core.power;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;

public class Utils {

	private Utils() {
	}

	/**
	 * Fills {@link ManagedSymmetricEss.ChannelId#DEBUG_SET_ACTIVE_POWER} and
	 * {@link ManagedSymmetricEss.ChannelId#DEBUG_SET_REACTIVE_POWER} of
	 * {@link MetaEss}.
	 * 
	 * @param esss a list of {@link ManagedSymmetricEss}
	 */
	public static void fillMetaEssDebugChannels(List<ManagedSymmetricEss> esss) {
		esss.stream() //
				.filter(MetaEss.class::isInstance) //
				.map(MetaEss.class::cast) //
				.forEach(metaEss -> {
					final var children = getChildren(esss, metaEss).toList();
					setValue(metaEss, ManagedSymmetricEss.ChannelId.DEBUG_SET_ACTIVE_POWER,
							sum(children, ManagedSymmetricEss::getDebugSetActivePowerChannel));
					setValue(metaEss, ManagedSymmetricEss.ChannelId.DEBUG_SET_REACTIVE_POWER,
							sum(children, ManagedSymmetricEss::getDebugSetReactivePowerChannel));
				});
	}

	private static Integer sum(List<ManagedSymmetricEss> children,
			Function<? super ManagedSymmetricEss, ? extends IntegerReadChannel> mapper) {
		return children.stream() //
				.map(mapper) //
				.map(IntegerReadChannel::getNextValue) //
				.map(Value::get) //
				.filter(Objects::nonNull) //
				.reduce(Integer::sum).orElse(null);
	}

	private static Stream<ManagedSymmetricEss> getChildren(List<ManagedSymmetricEss> esss, MetaEss ess) {
		return Arrays.stream(ess.getEssIds()) //
				.flatMap(e -> esss.stream() //
						.filter(i -> Objects.equals(i.id(), e))) //
				.flatMap(e -> e instanceof MetaEss me //
						? getChildren(esss, me) //
						: Stream.of(e));
	}
}
