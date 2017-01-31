package io.openems.api.channel;

public interface FunctionalChannelFunction<T> {

	public T handle(ReadChannel<T>... channels);
}
