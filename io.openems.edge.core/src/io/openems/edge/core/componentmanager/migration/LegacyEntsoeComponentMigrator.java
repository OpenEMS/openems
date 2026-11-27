package io.openems.edge.core.componentmanager.migration;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;
import static org.osgi.service.component.annotations.ServiceScope.SINGLETON;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Lazy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.logger.ContextLogger;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.controller.api.backend.api.DataFetchType;
import io.openems.edge.controller.api.backend.services.BackendConnectionState;
import io.openems.edge.controller.api.backend.services.BackendConnectionStateListener;
import io.openems.edge.core.appmanager.AppManagerUtil;

@Component(immediate = true, scope = SINGLETON)
public class LegacyEntsoeComponentMigrator {
	private static final String TARIFF_PROVIDER_COMPONENT_ID = "spotTariffProvider0";
	private final Logger log = new ContextLogger(LegacyEntsoeComponentMigrator.class, "LegacyMigration");

	@Reference(//
			cardinality = OPTIONAL, //
			policy = DYNAMIC, //
			policyOption = GREEDY, //
			target = "(&(service.factoryPid=TimeOfUseTariff.ENTSO-E))", //
			bind = "bindOldTouEntsoe", //
			unbind = "unbindOldTouEntsoe" //
	)
	private volatile OpenemsComponent oldTouEntsoe;

	protected void bindOldTouEntsoe(OpenemsComponent component) {
		this.tryToMigrate();
	}

	protected void unbindOldTouEntsoe(OpenemsComponent component) {
		// Nothing
	}

	@Reference(//
			cardinality = OPTIONAL, //
			policy = DYNAMIC, //
			policyOption = GREEDY, //
			target = "(&(service.factoryPid=Tariff.ENTSO-E.GridSell))", //
			bind = "bindEntsoeGridSell", //
			unbind = "unbindEntsoeGridSell" //
	)
	private volatile OpenemsComponent oldEntsoeGridSell;

	protected void bindEntsoeGridSell(OpenemsComponent component) {
		this.tryToMigrate();
	}

	protected void unbindEntsoeGridSell(OpenemsComponent component) {
		// Nothing
	}

	@Reference(//
			cardinality = OPTIONAL, //
			policy = DYNAMIC, //
			policyOption = GREEDY, //
			target = "(&(!(service.factoryPid=TimeOfUseTariff.ENTSO-E)))", //
			bind = "bindSpotTariffProvider", //
			unbind = "unbindSpotTariffProvider" //
	)
	private volatile SpotTariffProvider spotTariffProvider;

	protected void bindSpotTariffProvider(SpotTariffProvider spotTariffProvider) {
		this.tryToMigrate();
	}

	protected void unbindSpotTariffProvider(SpotTariffProvider spotTariffProvider) {
		// Nothing
	}

	@Reference
	private BackendConnectionStateListener backendConnectionStateListener;

	@Reference
	private SyncableRepository syncableRepository;

	@Reference
	private AppManagerUtil appManagerUtil;

	@Reference
	private ComponentManager componentManager;

	private final Consumer<BackendConnectionState> onConnectionStateChangeFunc = state -> this.tryToMigrate();
	private final Lazy<ExecutorService> migrationExecutor = Lazy.lazy(() -> Executors
			.newSingleThreadExecutor(Thread.ofPlatform().name("LegacyEntsoeComponentMigrator").factory()));
	private final Set<String> migratedComponentIds = new HashSet<>();

	private String createdSpotTariffProviderId;
	private boolean activated = false;

	@Activate
	protected void activate() {
		this.activated = true;
		this.backendConnectionStateListener.getConnectionState().subscribe(this.onConnectionStateChangeFunc);
		// onConnectionStateChangeFunc is called on subscription, so tryToMigrate() is
		// called automatically
	}

	@Deactivate
	protected void deactivate() throws InterruptedException {
		this.activated = false;
		this.backendConnectionStateListener.getConnectionState().unsubscribe(this.onConnectionStateChangeFunc);
		this.stopExecutor();
	}

	private void stopExecutor() throws InterruptedException {
		if (!this.migrationExecutor.isInitialized()) {
			return;
		}

		var executor = this.migrationExecutor.get();
		this.migrationExecutor.set(null);

		executor.shutdownNow();
		executor.awaitTermination(10, TimeUnit.SECONDS);
	}

	protected synchronized void tryToMigrate() {
		if (!this.activated) {
			return;
		}

		if (this.oldTouEntsoe != null) {
			this.execute("TimeOfUseTariff.ENTSO-E", this.oldTouEntsoe, this::migrateGridBuy);
		}
		if (this.oldEntsoeGridSell != null) {
			this.execute("Tariff.ENTSO-E.GridSell", this.oldEntsoeGridSell, this::migrateGridSell);
		}
	}

	protected <T extends OpenemsComponent> void execute(String name, T component, ThrowingConsumer<T, Exception> call) {
		this.migrationExecutor.get().execute(() -> {
			try {
				Thread.sleep(1000L); // Wait a bit for osgi to settle before the migration begins
				call.accept(component);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				this.log.error("Error during migration of {}", name, e);
			}
		});
	}

	protected boolean isAppInstalled(String appId) {
		return !this.appManagerUtil.getInstantiatedAppsOfApp(appId).isEmpty();
	}

	private boolean isGridBuyAppInstalled() {
		return this.isAppInstalled("App.TimeOfUseTariff.ENTSO-E");
	}

	private boolean isEeg2025AppInstalled() {
		return this.isAppInstalled("App.Tariff.Manual.EEG2025.GridSell");
	}

	private boolean isAppWithSpotTariffDependencyInstalled() {
		return this.isGridBuyAppInstalled() || this.isEeg2025AppInstalled();
	}

	private CompletableFuture<DataFetchType> determinateSpotPricesFetchType() {
		return switch (this.backendConnectionStateListener.getConnectionState().getValue()) {
		case BackendConnectionState.Connected() -> {
			// Test if prices are available
			var subscription = this.syncableRepository.subscribe(new SyncableKey.SpotTariff(BiddingZone.GERMANY));
			yield subscription.getEntry().waitForNonNullValue(Duration.ofSeconds(10)) //
					.thenApply(value -> value.value() != null ? DataFetchType.FETCH_VIA_BACKEND : DataFetchType.WAITING) //
					.exceptionally(ex -> DataFetchType.WAITING) //
					.whenComplete((result, ex) -> subscription.dispose());
		}
		case BackendConnectionState.NoConnection(Duration durationWithoutConnection) ->
			CompletableFuture.completedFuture(DataFetchType.WAITING);
		case BackendConnectionState.NoBackendInstance() ->
			CompletableFuture.completedFuture(DataFetchType.FETCH_LOCALLY);
		case BackendConnectionState.StillInitializing() -> CompletableFuture.completedFuture(DataFetchType.WAITING);
		};
	}

	private void migrateGridBuy(OpenemsComponent oldComponent) throws Exception {
		if (this.oldTouEntsoe != oldComponent || this.migratedComponentIds.contains(oldComponent.id())) {
			return;
		}

		var oldProperties = oldComponent.getComponentContext().getProperties();

		if (this.spotTariffProvider == null) {
			if (this.isGridBuyAppInstalled()) {
				this.log.warn(
						"Found deprecated TimeOfUseTariff.ENTSO-E component, but there is currently no spot tariff provider. Wait for spot tariff provider to appear. Should be automatically created by the app manager.");
				return;
			}

			var spotTariffComponentId = this
					.getOrCreateSpotTariffComponent((String) oldProperties.get("securityToken"));
			if (spotTariffComponentId == null) {
				this.log.warn(
						"Found deprecated TimeOfUseTariff.ENTSO-E component, but there is currently no spot tariff provider and I'm not able to create one, maybe because backend is enabled but it does not provide prices.");
				return;
			}
		}

		final var properties = new UpdatePropertiesBuilder() //
				.add("id", oldComponent.id()) //
				.add("alias", oldComponent.alias()) //
				.add("enabled", oldComponent.isEnabled()) //
				.add("biddingZone", (String) oldProperties.get("biddingZone")) //
				.add("ancillaryCosts", (String) oldProperties.get("ancillaryCosts")) //
				.add("calculateExpression", (String) oldProperties.get("calculateExpression")) //
				.add("service_ranking", 10) //
				.build();

		this.log.info("Migrating TimeOfUseTariff.ENTSO-E to Tariff.BySpotTariff.GridBuy ...");
		this.componentManager.handleDeleteComponentConfigRequest(null,
				new DeleteComponentConfig.Request(oldComponent.id()));
		this.createConfiguration("Tariff.BySpotTariff.GridBuy", properties);
		this.migratedComponentIds.add(oldComponent.id());
	}

	private void migrateGridSell(OpenemsComponent oldComponent) throws Exception {
		if (this.oldEntsoeGridSell != oldComponent || this.migratedComponentIds.contains(oldComponent.id())) {
			return;
		}

		var oldProperties = oldComponent.getComponentContext().getProperties();

		if (this.spotTariffProvider == null) {
			if (this.isAppWithSpotTariffDependencyInstalled()) {
				this.log.warn(
						"Found deprecated Tariff.ENTSO-E.GridSell component, but there is currently no spot tariff provider. Wait for spot tariff provider to appear.");
				return;
			}

			var spotTariffComponentId = this
					.getOrCreateSpotTariffComponent((String) oldProperties.get("securityToken"));
			if (spotTariffComponentId == null) {
				this.log.warn(
						"Found deprecated Tariff.ENTSO-E.GridSell component, but there is currently no spot tariff provider and I'm not able to create one, maybe because backend is enabled but it does not provide prices.");
				return;
			}
		}

		final var properties = new UpdatePropertiesBuilder() //
				.add("id", oldComponent.id()) //
				.add("alias", oldComponent.alias()) //
				.add("enabled", oldComponent.isEnabled()) //
				.add("biddingZone", (String) oldProperties.get("biddingZone")) //
				.add("calculateExpression", (String) oldProperties.get("calculateExpression")) //
				.add("service_ranking", 10) //
				.build();

		this.log.info("Migrating Tariff.ENTSO-E.GridSell to Tariff.BySpotTariff.GridSell ...");
		this.componentManager.handleDeleteComponentConfigRequest(null,
				new DeleteComponentConfig.Request(oldComponent.id()));
		this.createConfiguration("Tariff.BySpotTariff.GridSell", properties);
		this.migratedComponentIds.add(oldComponent.id());
	}

	private String getOrCreateSpotTariffComponent(String entsoeSecurityToken) throws Exception {
		if (this.spotTariffProvider != null) {
			return ((OpenemsComponent) this.spotTariffProvider).id();
		}
		if (this.createdSpotTariffProviderId != null) {
			return this.createdSpotTariffProviderId;
		}

		var fetchType = this.determinateSpotPricesFetchType().get();

		if (this.spotTariffProvider != null) {
			// Check again in case the spotTariffProvider was created while waiting for the
			// fetchType
			return ((OpenemsComponent) this.spotTariffProvider).id();
		}

		this.createdSpotTariffProviderId = this.createSpotTariffComponent(entsoeSecurityToken, fetchType);
		return this.createdSpotTariffProviderId;
	}

	private String createSpotTariffComponent(String entsoeSecurityToken, DataFetchType fetchType) throws Exception {
		return switch (fetchType) {
		case WAITING -> null;
		case FETCH_LOCALLY -> {
			final var properties = new UpdatePropertiesBuilder() //
					.add("id", TARIFF_PROVIDER_COMPONENT_ID) //
					.add("alias", "") //
					.add("enabled", true) //
					.add("securityToken", entsoeSecurityToken) //
					.add("service_ranking", 80) //
					.build();
			this.createConfiguration("Tariff.SpotTariff.ENTSO-E", properties);
			yield TARIFF_PROVIDER_COMPONENT_ID;
		}
		case FETCH_VIA_BACKEND -> {
			final var properties = new UpdatePropertiesBuilder() //
					.add("id", TARIFF_PROVIDER_COMPONENT_ID) //
					.add("alias", "") //
					.add("enabled", true) //
					.add("service_ranking", 100) //
					.build();
			this.createConfiguration("Tariff.SpotTariff.FromBackend", properties);
			yield TARIFF_PROVIDER_COMPONENT_ID;
		}
		};
	}

	/**
	 * Creates a Component configuration.
	 *
	 * @param factoryPid the Factory-PID
	 * @param properties the Component properties
	 */
	protected void createConfiguration(String factoryPid, List<UpdateComponentConfigRequest.Property> properties)
			throws Exception {
		try {
			this.log.atInfo() //
					.setMessage("Creating Component configuration [{}]: {}") //
					.addArgument(factoryPid) //
					.addArgument(() -> properties.stream() //
							.map(p -> p.name() + ":" + p.value().toString()) //
							.collect(Collectors.joining(", "))) //
					.log();
			this.componentManager.handleCreateComponentConfigRequest(null /* no user */,
					new CreateComponentConfig.Request(factoryPid, properties));
		} catch (OpenemsError.OpenemsNamedException e) {
			throw new Exception(String.format("Unable to create Component configuration for Factory [%s]", factoryPid),
					e);
		}
	}

	private class UpdatePropertiesBuilder {
		private final ImmutableList.Builder<UpdateComponentConfigRequest.Property> properties = ImmutableList.builder();

		public UpdatePropertiesBuilder add(String key, String value) {
			if (value != null) {
				this.properties.add(new UpdateComponentConfigRequest.Property(key, value));
			}
			return this;
		}

		public UpdatePropertiesBuilder add(String key, boolean value) {
			this.properties.add(new UpdateComponentConfigRequest.Property(key, value));
			return this;
		}

		public UpdatePropertiesBuilder add(String key, Number value) {
			if (value != null) {
				this.properties.add(new UpdateComponentConfigRequest.Property(key, value));
			}
			return this;
		}

		public List<UpdateComponentConfigRequest.Property> build() {
			return this.properties.build();
		}
	}

}
