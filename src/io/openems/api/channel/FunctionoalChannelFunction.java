package io.openems.api.channel;

public interface FunctionoalChannelFunction<T> {

	public T handle(ReadChannel<T>... channels);
}
