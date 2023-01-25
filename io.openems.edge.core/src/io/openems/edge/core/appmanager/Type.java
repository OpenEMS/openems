package io.openems.edge.core.appmanager;

import java.util.EnumMap;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParamter;

public interface Type<P extends Enum<P> & Type<P, A, M>, //
		A extends AbstractOpenemsAppWithProps<A, P, M>, //
		M extends io.openems.edge.core.appmanager.Type.Parameter> //
		extends Self<P> {

	public class Parameter {

		public static class BundleParamter extends Parameter {
			public final ResourceBundle bundle;

			public BundleParamter(ResourceBundle bundle) {
				this.bundle = bundle;
			}

			public final ResourceBundle getBundle() {
				return this.bundle;
			}

		}

		/**
		 * Creates a {@link BundleParamter} of a {@link ResourceBundle}.
		 * 
		 * @param bundle the {@link ResourceBundle}
		 * @return the {@link BundleParamter}
		 */
		public static final BundleParamter of(ResourceBundle bundle) {
			return new BundleParamter(bundle);
		}

		/**
		 * Creates a {@link BundleParamter} of a {@link ResourceBundle}.
		 * 
		 * @param bundle the {@link ResourceBundle}
		 * @return the {@link BundleParamter}
		 */
		public static final Supplier<BundleParamter> supplierOf(ResourceBundle bundle) {
			return () -> of(bundle);
		}

		/**
		 * Creates a {@link Function} to get a instance of {@link BundleParamter}.
		 * 
		 * @param <APP>                  the type of the {@link OpenemsApp}
		 * @param resourceBundleSupplier the supplier to get the {@link ResourceBundle}
		 * @return the {@link Function}
		 */
		public static final <APP> Function<GetParameterValues<APP>, BundleParamter> functionOf(
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
	public AppDef<A, P, M> def();

	/**
	 * Gets the name of the property.
	 * 
	 * @return the name
	 */
	public String name();

	/**
	 * Gets the mapped {@link JsonPrimitive} from a {@link EnumMap}.
	 * 
	 * @param map the {@link EnumMap}
	 * @return the {@link JsonPrimitive}
	 * @throws OpenemsNamedException on error
	 */
	public default JsonPrimitive from(EnumMap<P, JsonElement> map) throws OpenemsNamedException {
		return EnumUtils.getAsPrimitive(map, this.self());
	}

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
	 * {@link BundleParamter#bundle} will get returned.
	 * 
	 * @return the function
	 */
	public default Function<M, ResourceBundle> translationBundleSupplier() {
		return p -> {
			if (p instanceof BundleParamter) {
				return ((BundleParamter) p).bundle;
			}
			return null;
		};
	}

}