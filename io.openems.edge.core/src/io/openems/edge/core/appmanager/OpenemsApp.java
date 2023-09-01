package io.openems.edge.core.appmanager;

import org.osgi.service.component.ComponentConstants;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.flag.Flag;
import io.openems.edge.core.appmanager.flag.Flags;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

public interface OpenemsApp {

	/**
	 * Gets the {@link AppAssistant} for this {@link OpenemsApp}.
	 *
	 * @param user the {@link User}
	 * @return the AppAssistant
	 */
	public AppAssistant getAppAssistant(User user);

	/**
	 * Gets the {@link AppConfiguration} needed for the {@link OpenemsApp}.
	 *
	 * @param target   the {@link ConfigurationTarget}
	 * @param config   the configured app 'properties'
	 * @param language the language of the configuration
	 * @return the app Configuration
	 */
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config, Language language)
			throws OpenemsNamedException;

	/**
	 * Gets the unique App-ID of the {@link OpenemsApp}.
	 *
	 * @return a unique PID, usually the {@link ComponentConstants#COMPONENT_NAME}
	 *         of the OSGi Component provider.
	 */
	public String getAppId();

	/**
	 * Gets the {@link AppDescriptor} of the {@link OpenemsApp}.
	 *
	 * @return the {@link AppDescriptor}
	 */
	public AppDescriptor getAppDescriptor();

	/**
	 * Gets the {@link OpenemsAppCategory} of the {@link OpenemsApp}.
	 *
	 * @return the category's
	 */
	public OpenemsAppCategory[] getCategories();

	/**
	 * Gets the image of the {@link OpenemsApp} in Base64 encoding.
	 *
	 * @return a image representing the {@link OpenemsApp}
	 */
	public String getImage();

	/**
	 * Gets the name of the {@link OpenemsApp}.
	 *
	 * @param language the language of the name
	 * @return a human readable name
	 */
	public String getName(Language language);

	/**
	 * Gets the short name of the {@link OpenemsApp}.
	 * 
	 * @param language the language of the name
	 * @return a human readable short name; can be null
	 */
	public String getShortName(Language language);

	/**
	 * Gets the {@link OpenemsAppCardinality} of the {@link OpenemsApp}.
	 *
	 * @return the usage
	 */
	public OpenemsAppCardinality getCardinality();

	/**
	 * Gets all {@link OpenemsAppPropertyDefinition} of the app.
	 * 
	 * @return the {@link OpenemsAppPropertyDefinition
	 *         OpenemsAppPropertyDefinitions}
	 * @throws UnsupportedOperationException if not implemented
	 */
	public OpenemsAppPropertyDefinition[] getProperties();

	/**
	 * Gets the {@link ValidatorConfig} of this {@link OpenemsApp}.
	 *
	 * @return the ValidatorConfig
	 */
	public ValidatorConfig getValidatorConfig();

	/**
	 * Gets the {@link OpenemsAppPermissions} of this {@link OpenemsApp}.
	 * 
	 * @return the permissions
	 */
	public OpenemsAppPermissions getAppPermissions();

	/**
	 * Gets {@link Flag Flags} for this {@link OpenemsApp}. A Flag could be anything
	 * that would describe the app more.
	 * 
	 * <p>
	 * Flags may be specific for Monitoring e. g. only show the app after a key was
	 * entered ({@link Flags#SHOW_AFTER_KEY_REDEEM}).
	 * 
	 * @return an array of {@link Flag Flags}
	 */
	public default Flag[] flags() {
		return new Flag[] {};
	}

	/**
	 * Validate the {@link OpenemsApp}.
	 *
	 * @param instance the app instance
	 */
	public void validate(OpenemsAppInstance instance) throws OpenemsNamedException;

	public static final String FALLBACK_IMAGE = """
			data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY5\
			1AAABhWlDQ1BJQ0MgUHJvZmlsZQAAKM+VkT1Iw1AUhU9TpVIqgu0g4pChOlkQFXGUKBbBQmkrtOpg8tI/aNKQpLg4Cq4FB38Wqw\
			4uzro6uAqC4A+Io5OToouUeF9SaBEqeOHxPs5753DffYDQqDDN6pkANN02U3FJzOZWxcArfAghiEGEZWYZifRiBl3r655uU93Fe\
			Bb+V/1q3mKATySeY4ZpE28Qz2zaBud94ggrySrxOfG4SQ0SP3Jd8fiNc9FlgWdGzExqnjhCLBY7WOlgVjI14mniqKrplC9kPVY5\
			b3HWKjXW6pO/MJTXV9JcpzWCOJaQQBIiFNRQRgU2YrTrpFhI0bnUxT/s+pPkUshVBiPHAqrQILt+8D/4PVurMDXpJYUkoPfFcT5\
			GgcAu0Kw7zvex4zRPAP8zcKW3/dUGMPtJer2tRY+AgW3g4rqtKXvA5Q4w9GTIpuxKflpCoQC8n9E35YDwLRBc8+bWOsfpA5ChWS\
			3fAAeHwFiRste7vLuvc25/3nHnB+kHJSZyiKHoIYQAAAAJcEhZcwAADsQAAA7EAZUrDhsAADEISURBVHhe7Z0JmCVVlaDPe7lVF\
			UmJCAIqKuKKS4MMsilbVZajjmvj1oJAiy0qkFCVWYKgQCMo1AK0MCh0i5+oiIg66DgzVIIgUirqiA4O7QIiIo2KyFZ7Zr4+JyJO\
			5nn33Rvbi3jLjfN/36k499z93hPnRUS9fAGKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoii\
			KoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoi\
			iKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKkkgtOnadRqMRaYqi9AO1WufDRz06Koqi9DwasBRF6Rs0YCmK0\
			jdowFIUpW/QgKUoSt+gAUtRlL5BA5aiKH2Dt9/Dir4jkjUgz6Ik1ZFl2tH95dATB2Fk0ZWovR539kz4ybVXwiP3h3mtlLlW1AZB\
			7djalvmErQyRpNORiNPzYuuPcOkBeD5x36XRje9h+RywXJsZu9EJsBPY2pN5aXU6SqTNls9wntkewbqtHVMnzDxZRhLXzry+bPJ\
			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\
			1p8+JVMVTfD5D01wuyTJCL/Z/LJXOcMc79qVvh/crenmfAp8DFs2NnMB0BJtjkC1ciyBW9cy3PZRs9PNJz/7KRHquD89+XodYfA\
			5YBM3PdAQ5Z9bnbRqr+hm5t/2IxTdzOWS/r4MTbyeGUJDiQOWap7efRErfIf2ViPSWK6xK+6zPAYvmRhK3wcb8m53jVdff+dZIV\
			fqDfj6Z2V8ZwzetWAKc36RZlH6H5sibmWVTn3nH3+/9zUhX+oNu+TP1247E0HJLKMu7dG/xeZIUnDhA8TzN+RoBrMk5/oiiT7SU\
			LEh/itNNYaQeEfvQ3VLeb3wOWDQ3kjiHMOaf639kOg3PK6/4TLdPYLm+rNOYzHXnvWBhzHI2KhekJGkWqN+hOdImmxst05EuLqh\
			ClezSqVgvSgibPU7axdamSnsSh5mfVN4g9pZQYvFn/8i4eH0FbRpvHM2ThXHofJUVOIqrfFGU0WanME8QM81I3SRNnTRtuewM5S\
			eVScI1jjTtcv8sReHyz372q1i8nRhCc+P5sZNIZ3E4TuxjqySncOUn1esERfXL60btST0N5vpzvTw6I+2EmWdCNrabOmPTZR8u3\
			QWVkaLkxOfFk05nwzL3xGdY1CbXkzpBunRu2b9L7yRF9WvOmSlap/Fm1QlKmzod0+iE1Ampd4/kx6s0bkbqXtEbm1EevHE8z7j5\
			Ylm+umryDnPzXY7h0qtCnjnHrZkraOTVqf2sdSVd2lP0Rfo13tgL/4A0c+h7vJ0YQnPj+aVxtvr8z8s0eUdZjtClEyCWduaaZ22\
			4jgwmcl1cehxp6qRtS5JnfsWQHKwqQ/c2oXykU6abp/x5GftvzOdxdBe9uPY8PzoWOdck5Fok6XJcNp3KZdEJqfcYFK2cEcs1hx\
			6eT3uwE/hI9rlJv+jsD+z3muP1sl/IsZk6r1danZH2MuE+ZF8JOn1wtjyiSFPfS+SmKbNyv61XWEnrZZ4EjEtnspQtmnb6Nk+Qo\
			k+YuPZkHus0ZqkzaeaVZr7twn2kGVukt1xhkZ3Lxele4u3EkOwnT/Ivjia1aTuJCJfeC9jGRkfXOOPsvIBmfVcdxlbWtJltyL7k\
			xpFu1mVcusRlL5Kixkc6p03dSxLP0E5B7zgrUqhJFNo4vqZ26TJ9HwpzK4qtDsE2oh3dHEevCK2dzU4i8+J0V55NbGVlG1I3xWa\
			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\
			ClHwN2Q0YgCYxAP0mSIU/T/w6LEdvp3lxYAv5E3bzMdj46Ofg+1f69pyqU3sYB++vV3R7UcukzLmlccg4h6G6/eRQ7rmOTb4Ygl\
			dlNSgoRcYG/e7Uv+At3tegVqcXk4o82ILpT8PM9Hlw00V5XqOlpEMDVp/hmpsMNmkCjwnXYYew6WnazNN3XuQYy2Fs8nXYCwaum\
			ryKMmjcALOzEzC15reRoVOUP//ew8v5duqE6QauuclA0W7QyOoUacbUKcw+s45Blg/1g44dgtGdo9e9y+dUjbugUVsOt185BRvp\
			O6hWqA3CHIPLXhSt82jFZU/CbJsoax4m3J9XdGrxukGvzc3m9GwzHTtOpyOTtg5h6lngehKy2dtZcupOMDB4DhZ5G94WngsbHr4\
			C1l+V/D+I/uNes+LpVD+KoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoi\
			iKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo\
			iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKongEwH8CPRgCMWTNy0IAAAAASUVO\
			RK5CYII=""";

}
