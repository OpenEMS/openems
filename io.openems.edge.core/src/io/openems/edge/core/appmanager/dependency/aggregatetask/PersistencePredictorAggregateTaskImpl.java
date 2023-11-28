package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;

@Component(//
		service = { //
				AggregateTask.class, //
				PersistencePredictorAggregateTask.class, //
				PersistencePredictorAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class PersistencePredictorAggregateTaskImpl implements PersistencePredictorAggregateTask {

	private ComponentManager componentManager;

	private Set<String> channelsToAdd;
	private Set<String> channelsToRemove;

	@Activate
	public PersistencePredictorAggregateTaskImpl(//
			@Reference ComponentManager componentManager //
	) {
		this.componentManager = componentManager;
	}

	@Override
	public void aggregate(//
			PersistencePredictorConfiguration currentConfiguration, //
			PersistencePredictorConfiguration lastConfiguration //
	) {
		if (lastConfiguration != null) {
			this.channelsToRemove.addAll(lastConfiguration.channels());
		}
		if (currentConfiguration != null) {
			this.channelsToRemove.removeAll(currentConfiguration.channels());
			this.channelsToAdd.addAll(currentConfiguration.channels());
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		updatePredictor(this.componentManager, user, this.channelsToAdd, this.channelsToRemove, otherAppConfigurations);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		updatePredictor(this.componentManager, user, emptySet(), this.channelsToRemove, otherAppConfigurations);
	}

	@Override
	public void validate(//
			List<String> errors, //
			AppConfiguration appConfiguration, //
			PersistencePredictorConfiguration config //
	) {
		final Set<String> existingChannels;
		try {
			existingChannels = this.getExistingPredictorChannels();
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return;
		}

		final var missingChannels = new HashSet<>(config.channels());
		missingChannels.removeAll(existingChannels);

		if (missingChannels.isEmpty()) {
			return;
		}
		errors.add("Missing channels in predictor [" + String.join(";", missingChannels) + "]");
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdatePredictor");
	}

	@Override
	public void reset() {
		this.channelsToAdd = new HashSet<String>();
		this.channelsToRemove = new HashSet<String>();
	}

	private static void updatePredictor(//
			ComponentManager componentManager, //
			User user, //
			Set<String> channelsToAdd, //
			Set<String> channelsToRemove, //
			List<AppConfiguration> otherAppConfigurations //
	) throws OpenemsNamedException {
		if (channelsToAdd.isEmpty() && channelsToRemove.isEmpty()) {
			return;
		}

		final var usedChannelsOfOtherConfigs = getAllChannels(otherAppConfigurations);
		channelsToRemove = new HashSet<>(channelsToRemove);
		channelsToRemove.removeAll(usedChannelsOfOtherConfigs);

		final var predictors = getPredictors(componentManager);
		if (predictors.isEmpty() && channelsToAdd.isEmpty()) {
			return;
		}
		if (predictors.size() != 1) {
			throw new OpenemsException("Not exactly one Persistence Predictor available");
		}
		final var predictor = predictors.get(0);

		final var existingChannels = getExistingPredictorChannels(predictor);

		if (existingChannels.containsAll(channelsToAdd) //
				&& !channelsToRemove.stream().anyMatch(existingChannels::contains)) {
			return;
		}

		existingChannels.addAll(channelsToAdd);
		existingChannels.removeAll(channelsToRemove);

		try {
			componentManager.handleJsonrpcRequest(user, new UpdateComponentConfigRequest(predictor.id(), List.of(//
					new UpdateComponentConfigRequest.Property("channelAddresses", existingChannels.stream() //
							.map(JsonPrimitive::new) //
							.collect(toJsonArray())) //
			))).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new OpenemsException("Unable to update Persistence Predictor", e);
		}
	}

	private static Set<String> getAllChannels(List<AppConfiguration> otherAppConfigurations) {
		return AppConfiguration
				.flatMap(otherAppConfigurations, PersistencePredictorAggregateTask.class,
						PersistencePredictorConfiguration::channels)//
				.collect(toSet());
	}

	private Set<String> getExistingPredictorChannels() throws OpenemsNamedException {
		return getExistingPredictorChannels(this.getPredictor());
	}

	private static Set<String> getExistingPredictorChannels(OpenemsComponent predictor) throws OpenemsNamedException {
		return Optional.ofNullable((Object[]) predictor.getComponentContext().getProperties().get("channelAddresses")) //
				.map(t -> Stream.of(t).map(String.class::cast).collect(toSet())) //
				.orElse(emptySet());
	}

	private OpenemsComponent getPredictor() throws OpenemsNamedException {
		final var persistencePredictors = getPredictors(this.componentManager);

		if (persistencePredictors.size() != 1) {
			throw new OpenemsException("Not exactly one Persistence Predictor available");
		}
		return persistencePredictors.get(0);
	}

	private static List<OpenemsComponent> getPredictors(ComponentManager componentManager)
			throws OpenemsNamedException {
		return componentManager.getAllComponents().stream() //
				.filter(c -> "Predictor.PersistenceModel".equals(c.serviceFactoryPid())) //
				.toList();
	}

}
