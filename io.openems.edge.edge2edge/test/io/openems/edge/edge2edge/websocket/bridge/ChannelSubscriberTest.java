package io.openems.edge.edge2edge.websocket.bridge;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.types.ChannelAddress;

class ChannelSubscriberTest {

	private static final ChannelAddress DUMMY_CHANNEL_1 = new ChannelAddress("cmp0", "Channel0");
	private static final ChannelAddress DUMMY_CHANNEL_2 = new ChannelAddress("cmp0", "Channel1");
	private static final ChannelAddress DUMMY_CHANNEL_3 = new ChannelAddress("cmp0", "Channel2");

	private ChannelSubscriber channelSubscriber;

	@BeforeEach
	void setUp() {
		this.channelSubscriber = new ChannelSubscriber();
	}

	@Test
	void testSetSubscribeChannels() {
		this.channelSubscriber.setSubscribeChannels(Set.of(DUMMY_CHANNEL_1));
		assertThat(this.channelSubscriber.getSubscribedChannels(), hasItem(equalTo(DUMMY_CHANNEL_1)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testSubscribeChannelsList() {
		this.channelSubscriber.subscribeChannels(List.of(DUMMY_CHANNEL_1));
		assertThat(this.channelSubscriber.getSubscribedChannels(), hasItem(equalTo(DUMMY_CHANNEL_1)));

		this.channelSubscriber.subscribeChannels(List.of(DUMMY_CHANNEL_2, DUMMY_CHANNEL_3));
		assertThat(this.channelSubscriber.getSubscribedChannels(),
				hasItems(equalTo(DUMMY_CHANNEL_1), equalTo(DUMMY_CHANNEL_2), equalTo(DUMMY_CHANNEL_2)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testSubscribeChannelsSet() {
		this.channelSubscriber.subscribeChannels(Set.of(DUMMY_CHANNEL_1));
		assertThat(this.channelSubscriber.getSubscribedChannels(), hasItem(equalTo(DUMMY_CHANNEL_1)));

		this.channelSubscriber.subscribeChannels(Set.of(DUMMY_CHANNEL_2, DUMMY_CHANNEL_3));
		assertThat(this.channelSubscriber.getSubscribedChannels(),
				hasItems(equalTo(DUMMY_CHANNEL_1), equalTo(DUMMY_CHANNEL_2), equalTo(DUMMY_CHANNEL_3)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void testSubscribeChannel() {
		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_1);
		assertThat(this.channelSubscriber.getSubscribedChannels(), hasItem(equalTo(DUMMY_CHANNEL_1)));

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_2);
		assertThat(this.channelSubscriber.getSubscribedChannels(),
				hasItems(equalTo(DUMMY_CHANNEL_1), equalTo(DUMMY_CHANNEL_2)));
	}

	@Test
	void testUnsubscribeChannel() {
		this.channelSubscriber.setSubscribeChannels(Set.of(DUMMY_CHANNEL_1, DUMMY_CHANNEL_2));
		assertEquals(2, this.channelSubscriber.getSubscribedChannels().size());

		this.channelSubscriber.unsubscribeChannel(DUMMY_CHANNEL_2);
		assertThat(this.channelSubscriber.getSubscribedChannels(),
				allOf(hasItem(equalTo(DUMMY_CHANNEL_1)), everyItem(not(equalTo(DUMMY_CHANNEL_2)))));
	}

	@Test
	void testUnsubscribeChannels() {
		this.channelSubscriber.setSubscribeChannels(Set.of(DUMMY_CHANNEL_1, DUMMY_CHANNEL_2));
		assertEquals(2, this.channelSubscriber.getSubscribedChannels().size());

		this.channelSubscriber.unsubscribeChannels(Set.of(DUMMY_CHANNEL_1, DUMMY_CHANNEL_2));
		assertTrue(this.channelSubscriber.getSubscribedChannels().isEmpty());
	}

	@SuppressWarnings("unchecked")
	@Test
	void testUnsubscribeAll() {
		this.channelSubscriber.setSubscribeChannels(Set.of(DUMMY_CHANNEL_1, DUMMY_CHANNEL_2));
		assertEquals(2, this.channelSubscriber.getSubscribedChannels().size());

		assertThat(this.channelSubscriber.unsubscribeAll(),
				hasItems(equalTo(DUMMY_CHANNEL_1), equalTo(DUMMY_CHANNEL_2)));
		assertTrue(this.channelSubscriber.getSubscribedChannels().isEmpty());
	}

	@Test
	void testAddSubscribeListener() {
		final var queue = new LinkedList<Set<ChannelAddress>>();
		this.channelSubscriber.addSubscribeListener(queue::add);

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_1);
		assertThat(queue.poll(), hasItem(equalTo(DUMMY_CHANNEL_1)));

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_2);
		assertThat(queue.poll(), allOf(hasItem(equalTo(DUMMY_CHANNEL_2)), everyItem(not(equalTo(DUMMY_CHANNEL_1)))));

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_2);
		assertTrue(queue.isEmpty());
	}

	@Test
	void testRemoveSubscribeListener() {
		final var queue = new LinkedList<Set<ChannelAddress>>();
		final var listener = this.channelSubscriber.addSubscribeListener(queue::add);

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_1);
		assertThat(queue.poll(), hasItem(equalTo(DUMMY_CHANNEL_1)));

		assertTrue(this.channelSubscriber.removeSubscribeListener(listener));

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_2);
		assertTrue(queue.isEmpty());
	}

	@Test
	void testAddUnsubscribeListener() {
		final var queue = new LinkedList<Set<ChannelAddress>>();
		this.channelSubscriber.addUnsubscribeListener(queue::add);

		this.channelSubscriber.subscribeChannel(DUMMY_CHANNEL_1);
		assertTrue(queue.isEmpty());

		this.channelSubscriber.unsubscribeChannel(DUMMY_CHANNEL_1);
		assertThat(queue.poll(), hasItem(equalTo(DUMMY_CHANNEL_1)));

		this.channelSubscriber.unsubscribeChannel(DUMMY_CHANNEL_1);
		assertTrue(queue.isEmpty(), queue.toString());
	}

	@Test
	void testRemoveUnsubscribeListener() {
		final var queue = new LinkedList<Set<ChannelAddress>>();
		final var listener = this.channelSubscriber.addUnsubscribeListener(queue::add);

		this.channelSubscriber.subscribeChannels(Set.of(DUMMY_CHANNEL_1, DUMMY_CHANNEL_2));
		this.channelSubscriber.unsubscribeChannel(DUMMY_CHANNEL_2);
		assertThat(queue.poll(), hasItem(equalTo(DUMMY_CHANNEL_2)));

		assertTrue(this.channelSubscriber.removeUnsubscribeListener(listener));

		this.channelSubscriber.unsubscribeChannel(DUMMY_CHANNEL_1);
		assertTrue(queue.isEmpty());
	}

	@Test
	void testGetSubscribedChannels() {
		assertThrows(UnsupportedOperationException.class, () -> {
			this.channelSubscriber.getSubscribedChannels().add(DUMMY_CHANNEL_1);
		});
	}

}