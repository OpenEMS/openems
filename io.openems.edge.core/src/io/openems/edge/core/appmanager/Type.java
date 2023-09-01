package io.openems.edge.core.appmanager;

import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Supplier;

import io.openems.common.session.Language;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

public interface Type<P extends Nameable, //
		A extends OpenemsApp, //
		M> //
		extends Self<Type<P, A, M>>, Nameable {

	public static class AbstractType<P extends Nameable, //
			A extends OpenemsApp, //
			M> implements Type<P, A, M> {

		private final String name;
		private final AppDef<? super A, ? super P, ? super M> def;
		private final Function<GetParameterValues<A>, M> getParameterFunction;

		public AbstractType(String name, AppDef<? super A, ? super P, ? super M> def,
				Function<GetParameterValues<A>, M> getParameterFunction) {
			super();
			this.name = name;
			this.def = def;
			this.getParameterFunction = getParameterFunction;
		}

		@Override
		public Type<P, A, M> self() {
			return this;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public AppDef<? super A, ? super P, ? super M> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<A>, M> getParamter() {
			return this.getParameterFunction;
		}

	}

	public class Parameter {

		public static interface BundleProvider {

			/**
			 * Gets the {@link ResourceBundle} to get the translations from.
			 * 
			 * @return the {@link ResourceBundle}
			 */
			public ResourceBundle bundle();

		}

		public static class BundleParameter extends Parameter implements BundleProvider {
			public final ResourceBundle bundle;

			public BundleParameter(ResourceBundle bundle) {
				this.bundle = bundle;
			}

			@Override
			public ResourceBundle bundle() {
				return this.bundle;
			}

		}

		/**
		 * Creates a {@link BundleParameter} of a {@link ResourceBundle}.
		 * 
		 * @param bundle the {@link ResourceBundle}
		 * @return the {@link BundleParameter}
		 */
		public static final BundleParameter of(ResourceBundle bundle) {
			return new BundleParameter(bundle);
		}

		/**
		 * Creates a {@link BundleParameter} of a {@link ResourceBundle}.
		 * 
		 * @param bundle the {@link ResourceBundle}
		 * @return the {@link BundleParameter}
		 */
		public static final Supplier<BundleParameter> supplierOf(ResourceBundle bundle) {
			return () -> of(bundle);
		}

		/**
		 * Creates a {@link Function} to get a instance of {@link BundleParameter}.
		 * 
		 * @param <APP>                  the type of the {@link OpenemsApp}
		 * @param resourceBundleSupplier the supplier to get the {@link ResourceBundle}
		 * @return the {@link Function}
		 */
		public static final <APP> Function<GetParameterValues<APP>, BundleParameter> functionOf(
				Function<Language, ResourceBundle> resourceBundleSupplier) {
			return t -> of(resourceBundleSupplier.apply(t.language));
		}

		/**
		 * Creates a Empty {@link Parameter}.
		 * 
		 * @return the {@link Parameter}
		 */
		public static final Parameter empty() {
			return new Parameter();
		}

	}

	/**
	 * Gets the {@link AppDef} of the property.
	 * 
	 * @return the {@link AppDef}
	 */
	public AppDef<? super A, ? super P, ? super M> def();

	public static final class GetParameterValues<APP> {
		public final APP app;
		public final Language language;

		public GetParameterValues(APP app, Language language) {
			this.app = app;
			this.language = language;
		}

	}

	/**
	 * Gets a function to get the parameters.
	 * 
	 * @return the function
	 */
	public Function<GetParameterValues<A>, M> getParamter();

	/**
	 * Gets a function to get a {@link ResourceBundle} for translation.
	 * 
	 * <p>
	 * If the {@link Parameter} type is a BundleParamter,
	 * {@link BundleParameter#bundle} will get returned.
	 * 
	 * @return the function
	 */
	public default Function<M, ResourceBundle> translationBundleSupplier() {
		return p -> {
			if (p instanceof BundleParameter) {
				return ((BundleParameter) p).bundle;
			}
			return null;
		};
	}

}