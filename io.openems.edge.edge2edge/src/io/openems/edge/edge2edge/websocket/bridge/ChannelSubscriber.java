package io.openems.edge.edge2edge.websocket.bridge;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;

import io.openems.common.types.ChannelAddress;

public class ChannelSubscriber {

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final Set<ChannelAddress> subscribedChannels = new HashSet<>();
	private final List<Consumer<Set<ChannelAddress>>> onChannelsSubscribed = new CopyOnWriteArrayList<>();
	private final List<Consumer<Set<ChannelAddress>>> onChannelsUnsubscribed = new CopyOnWriteArrayList<>();

	/**
	 * Sets the subscribed channels.
	 * 
	 * @param channels to overwrite the current channels with
	 */
	public void setSubscribeChannels(Set<ChannelAddress> channels) {
		this.setChannels(channels);
	}

	/**
	 * Adds channels to the subscribed channels.
	 * 
	 * @param channels the channels to add
	 */
	public void subscribeChannels(Set<ChannelAddress> channels) {
		this.addChannels(channels);
	}

	/**
	 * Adds channels to the subscribed channels.
	 * 
	 * @param channels the channels to add
	 */
	public void subscribeChannels(List<ChannelAddress> channels) {
		this.addChannels(new HashSet<>(channels));
	}

	/**
	 * Adds channel to the subscribed channels.
	 * 
	 * @param channel the channel to add
	 */
	public void subscribeChannel(ChannelAddress channel) {
		this.addChannels(Set.of(channel));
	}

	/**
	 * Removes a channel from the subscribed channels.
	 * 
	 * @param channel the channel to remove
	 */
	public void unsubscribeChannel(ChannelAddress channel) {
		this.removeChannels(Set.of(channel));
	}

	/**
	 * Removes channels from the subscribed channels.
	 * 
	 * @param channels the channels to remove
	 */
	public void unsubscribeChannels(Set<ChannelAddress> channels) {
		this.removeChannels(channels);
	}

	/**
	 * Removes all channels from the subscribed channels.
	 * 
	 * @return all channels which got removed
	 */
	public Set<ChannelAddress> unsubscribeAll() {
		final Set<ChannelAddress> channels;
		this.readWriteLock.readLock().lock();
		try {
			channels = new HashSet<>(this.subscribedChannels);
			this.subscribedChannels.clear();

			this.notifyChannelsUnsubscribed(channels);
		} finally {
			this.readWriteLock.readLock().unlock();
		}
		return channels;
	}

	/**
	 * Adds a listener if the subscribed channels change. The event only contains
	 * the new subscribed channels.
	 * 
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public Consumer<Set<ChannelAddress>> addSubscribeListener(Consumer<Set<ChannelAddress>> listener) {
		this.onChannelsSubscribed.add(listener);
		return listener;
	}

	/**
	 * Removes a channels subscribe listener.
	 * 
	 * @param listener the listener to remove
	 * @return true if the listener got removed; else false
	 */
	public boolean removeSubscribeListener(Consumer<Set<ChannelAddress>> listener) {
		return this.onChannelsSubscribed.remove(listener);
	}

	/**
	 * Adds a listener if the subscribed channels change. The event only contains
	 * the unsubscribed channels.
	 * 
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public Consumer<Set<ChannelAddress>> addUnsubscribeListener(Consumer<Set<ChannelAddress>> listener) {
		this.onChannelsUnsubscribed.add(listener);
		return listener;
	}

	/**
	 * Removes a channels unsubscribe listener.
	 * 
	 * @param listener the listener to remove
	 * @return true if the listener got removed; else false
	 */
	public boolean removeUnsubscribeListener(Consumer<Set<ChannelAddress>> listener) {
		return this.onChannelsUnsubscribed.remove(listener);
	}

	/**
	 * Gets the currently subscribed channels.
	 * 
	 * @return the channels
	 */
	public Set<ChannelAddress> getSubscribedChannels() {
		this.readWriteLock.readLock().lock();
		try {
			return ImmutableSet.copyOf(this.subscribedChannels);
		} finally {
			this.readWriteLock.readLock().unlock();
		}
	}

	private void setChannels(Set<ChannelAddress> channels) {
		this.readWriteLock.writeLock().lock();
		try {
			final var removedChannels = new HashSet<>(this.subscribedChannels);
			removedChannels.removeAll(channels);

			this.subscribedChannels.removeAll(removedChannels);

			final var addedChannels = channels.stream() //
					.filter(this.subscribedChannels::add) //
					.collect(toSet());

			this.notifyChannelsSubscribed(addedChannels);
			this.notifyChannelsUnsubscribed(removedChannels);
		} finally {
			this.readWriteLock.writeLock().unlock();
		}
	}

	private void addChannels(Set<ChannelAddress> channels) {
		this.readWriteLock.writeLock().lock();
		try {
			final var changes = channels.stream() //
					.filter(this.subscribedChannels::add) //
					.collect(toSet());

			this.notifyChannelsSubscribed(changes);
		} finally {
			this.readWriteLock.writeLock().unlock();
		}
	}

	private void removeChannels(Set<ChannelAddress> channels) {
		this.readWriteLock.writeLock().lock();
		try {
			final var changes = channels.stream() //
					.filter(this.subscribedChannels::remove) //
					.collect(toSet());

			this.notifyChannelsUnsubscribed(changes);
		} finally {
			this.readWriteLock.writeLock().unlock();
		}
	}

	private void notifyChannelsSubscribed(Set<ChannelAddress> channels) {
		for (var subscriber : this.onChannelsSubscribed) {
			subscriber.accept(channels);
		}
	}

	private void notifyChannelsUnsubscribed(Set<ChannelAddress> channels) {
		for (var subscriber : this.onChannelsUnsubscribed) {
			subscriber.accept(channels);
		}
	}

}
