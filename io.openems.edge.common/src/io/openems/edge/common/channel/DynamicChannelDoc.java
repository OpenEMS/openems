package io.openems.edge.common.channel;

import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.channel.dynamicdoctext.DynamicDocText;
import io.openems.edge.common.channel.dynamicdoctext.ParameterProvider;
import io.openems.edge.common.type.TextProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DynamicChannelDoc<V> {
	/**
	 * Provides a callback on initialization of the actual Channel.
	 *
	 * @param callback the method to call on initialization
	 * @return myself
	 */
	public AbstractDoc<V> onInit(Consumer<Channel<V>> callback);

	public void setDynamicDocText(DynamicDocText dynamicDocText);

	public class DynamicChannelDocBuilder<T extends DynamicChannelDoc<?>> {
		private final Function<Consumer<T>, T> instanceCreator;
		private final List<Consumer<T>> factories;

		public DynamicChannelDocBuilder(Function<Consumer<T>, T> instanceCreator) {
			this.instanceCreator = instanceCreator;
			this.factories = new ArrayList<>();
		}

		private void addFactory(Consumer<T> factory) {
			this.factories.add(factory);
		}

		private Consumer<T> buildFactory() {
			return (doc) -> {
				for (Consumer<T> factory : this.factories) {
					factory.accept(doc);
				}
			};
		}

		public T build() {
			return this.instanceCreator.apply(this.buildFactory());
		}

		/**
		 * This function provides a way to set {@link Doc} attributes. It's important to
		 * set all doc data within this factory and not outside after building.
		 * 
		 * @param factory This factory function is called every time a new {@link Doc}
		 *                is created.
		 * @return myself
		 */
		public DynamicChannelDocBuilder<T> doc(Consumer<T> factory) {
			this.addFactory(factory);
			return this;
		}

		public DynamicChannelDocBuilder<T> setDynamicText(TextProvider text,
				ParameterProvider... parameterProviders) {
			this.addFactory(doc -> {
				doc.onInit(channel -> {
					var providerInstances = Arrays.stream(parameterProviders)
							.map(ParameterProvider::clone)
							.toArray(ParameterProvider[]::new);

					var dynamicDocText = new DynamicDocText(text, providerInstances);
					dynamicDocText.init(channel);

					doc.setDynamicDocText(dynamicDocText);
				});
			});
			return this;
		}
	}
}
