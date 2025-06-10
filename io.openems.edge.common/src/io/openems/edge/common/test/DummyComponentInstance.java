package io.openems.edge.common.test;

import org.osgi.service.component.ComponentInstance;

import io.openems.common.utils.FunctionUtils;

public class DummyComponentInstance<S> implements ComponentInstance<S> {

	public static class DummyComponentInstanceBuilder<S> {

		private Runnable dispose;
		private S instance;

		public DummyComponentInstanceBuilder<S> setDispose(Runnable dispose) {
			this.dispose = dispose;
			return this;
		}

		public DummyComponentInstanceBuilder<S> setInstance(S instance) {
			this.instance = instance;
			return this;
		}

		public DummyComponentInstance<S> build() {
			return new DummyComponentInstance<>(this.dispose, this.instance);
		}

	}

	/**
	 * Creates a builder for a {@link DummyComponentInstance}.
	 * 
	 * @param <S> the type of the service
	 * @return the builder
	 */
	public static <S> DummyComponentInstanceBuilder<S> create() {
		return new DummyComponentInstanceBuilder<>();
	}

	private final Runnable dispose;
	private final S instance;

	public DummyComponentInstance(Runnable dispose, S instance) {
		super();
		this.dispose = dispose != null ? dispose : FunctionUtils::doNothing;
		this.instance = instance;
	}

	@Override
	public void dispose() {
		this.dispose.run();
	}

	@Override
	public S getInstance() {
		return this.instance;
	}

}