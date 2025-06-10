package io.openems.edge.app.common.props;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.app.common.props.RelayProps.RelayContactFilter;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.io.api.DigitalOutput;

public class RelayContactFilterTest {

	private RelayContactFilter relayContactFilter;

	@Before
	public void setUp() {
		final Predicate<DigitalOutput> componentFilter = t -> true;
		final Function<DigitalOutput, String> componentAliasMapper = t -> t.alias();
		final BiPredicate<DigitalOutput, BooleanWriteChannel> channelFilter = (t, u) -> true;
		final BiFunction<DigitalOutput, BooleanWriteChannel, String> channelAliasMapper = (t, u) -> t.alias();
		final BiFunction<DigitalOutput, BooleanWriteChannel, List<String>> disabledReasons = (t, u) -> emptyList();

		this.relayContactFilter = RelayContactFilter.create() //
				.withComponentFilter(componentFilter) //
				.withComponentAliasMapper(componentAliasMapper) //
				.withChannelFilter(channelFilter) //
				.withChannelAliasMapper(channelAliasMapper) //
				.withDisabledReasons(disabledReasons);

		assertEquals(componentFilter, this.relayContactFilter.componentFilter());
		assertEquals(componentAliasMapper, this.relayContactFilter.componentAliasMapper());
		assertEquals(channelFilter, this.relayContactFilter.channelFilter());
		assertEquals(channelAliasMapper, this.relayContactFilter.channelAliasMapper());
		assertEquals(disabledReasons, this.relayContactFilter.disabledReasons());
	}

	@Test
	public void testWithComponentFilter() {
		final Predicate<DigitalOutput> componentFilter = t -> true;
		assertNotEquals(componentFilter, this.relayContactFilter.componentFilter());

		final var relayContactFilter = this.relayContactFilter.withComponentFilter(componentFilter);

		assertEquals(componentFilter, relayContactFilter.componentFilter());
	}

	@Test
	public void testWithComponentAliasMapper() {
		final Function<DigitalOutput, String> componentAliasMapper = t -> t.alias();
		assertNotEquals(componentAliasMapper, this.relayContactFilter.componentAliasMapper());

		final var relayContactFilter = this.relayContactFilter.withComponentAliasMapper(componentAliasMapper);

		assertEquals(componentAliasMapper, relayContactFilter.componentAliasMapper());
	}

	@Test
	public void testWithChannelFilter() {
		final BiPredicate<DigitalOutput, BooleanWriteChannel> channelFilter = (t, u) -> true;
		assertNotEquals(channelFilter, this.relayContactFilter.channelFilter());

		final var relayContactFilter = this.relayContactFilter.withChannelFilter(channelFilter);

		assertEquals(channelFilter, relayContactFilter.channelFilter());
	}

	@Test
	public void testWithChannelAliasMapper() {
		final BiFunction<DigitalOutput, BooleanWriteChannel, String> channelAliasMapper = (t, u) -> t.alias();
		assertNotEquals(channelAliasMapper, this.relayContactFilter.channelAliasMapper());

		final var relayContactFilter = this.relayContactFilter.withChannelAliasMapper(channelAliasMapper);

		assertEquals(channelAliasMapper, relayContactFilter.channelAliasMapper());
	}

	@Test
	public void testWithDisabledReasons() {
		final BiFunction<DigitalOutput, BooleanWriteChannel, List<String>> disabledReasons = (t, u) -> emptyList();
		assertNotEquals(disabledReasons, this.relayContactFilter.disabledReasons());

		final var relayContactFilter = this.relayContactFilter.withDisabledReasons(disabledReasons);

		assertEquals(disabledReasons, relayContactFilter.disabledReasons());
	}

}
