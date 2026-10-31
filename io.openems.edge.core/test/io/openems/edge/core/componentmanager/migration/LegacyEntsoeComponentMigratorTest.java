package io.openems.edge.core.componentmanager.migration;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Components.appManager;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Components.createSut;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Components.gridBuyComponent;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Components.gridSellComponent;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Components.syncableRepositoryWithTariffData;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Components.syncableRepositoryWithoutTariffData;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Matchers.expectComponent;
import static io.openems.edge.core.componentmanager.migration.LegacyEntsoeComponentMigratorTest.Matchers.expectSpotTariffProvider;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;

import io.openems.common.function.ThrowingConsumer;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.tariff.api.spottariff.dummy.DummySpotTariffProvider;
import io.openems.common.test.OsgiComponentTest;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.services.syncable.DummySyncableRepository;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.controller.api.backend.services.BackendConnectionState;
import io.openems.edge.controller.api.backend.services.BackendConnectionStateListener;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

class LegacyEntsoeComponentMigratorTest {

	private static final String ENTSOE_APP_ID = "App.TimeOfUseTariff.ENTSO-E";
	private static final String NEW_GRID_BUY_FACTORY_ID = "Tariff.BySpotTariff.GridBuy";
	private static final String NEW_GRID_SELL_FACTORY_ID = "Tariff.BySpotTariff.GridSell";
	private static final String NEW_ENTSOE_SPOT_TARIFF_FACTORY_ID = "Tariff.SpotTariff.ENTSO-E";
	private static final String NEW_BACKEND_SPOT_TARIFF_FACTORY_ID = "Tariff.SpotTariff.FromBackend";
	private static final String ENTSOE_TOKEN = "test-token";

	private static final String[] PROPERTIES_TO_COMPARE = new String[] { "id", "alias", "enabled", "biddingZone",
			"ancillaryCosts", "calculateExpression" };

	@Nested
	class WithApp {

		@Test
		void migrateGridBuyWithTariffProvider() throws Exception {
			final var oldComponent = gridBuyComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.Connected()) //
					.addReference("appManagerUtil", appManager(ENTSOE_APP_ID)) //
					.addReference("oldTouEntsoe", oldComponent) //
					.addReference("spotTariffProvider", mock(SpotTariffProvider.class)) //
					.activate(null);

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldComponent.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldComponent)));

			test.deactivate();
		}

		@Test
		void migrateGridBuyAndSellWithTariffProvider() throws Exception {
			final var oldBuy = gridBuyComponent();
			final var oldSell = gridSellComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.Connected()) //
					.addReference("appManagerUtil", appManager(ENTSOE_APP_ID)) //
					.addReference("oldTouEntsoe", oldBuy) //
					.addReference("oldEntsoeGridSell", oldSell) //
					.addReference("spotTariffProvider", mock(SpotTariffProvider.class)) //
					.activate(null);

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldBuy.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldBuy)));

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldSell.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_SELL_FACTORY_ID, oldSell)));

			test.deactivate();
		}

		@Test
		void dontMigrateGridBuyAndSellWithoutTariffProvider() throws Exception {
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.Connected()) //
					.addReference("appManagerUtil", appManager(ENTSOE_APP_ID)) //
					.addReference("oldTouEntsoe", gridBuyComponent()) //
					.addReference("oldEntsoeGridSell", gridSellComponent()) //
					.activate(null);

			verify(componentManager, never()).handleDeleteComponentConfigRequest(any(), any());
			verify(componentManager, never()).handleCreateComponentConfigRequest(any(), any());

			test.deactivate();
		}
	}

	@Nested
	class WithoutAppWithoutBackend {

		@Test
		void migrateGridBuyWithTariffProvider() throws Exception {
			final var oldComponent = gridBuyComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.NoBackendInstance()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("oldTouEntsoe", oldComponent) //
					.addReference("spotTariffProvider", mock(SpotTariffProvider.class)) //
					.activate(null);

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldComponent.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldComponent)));

			test.deactivate();
		}

		@Test
		void migrateGridBuyWithoutTariffProvider() throws Exception {
			final var oldComponent = gridBuyComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.NoBackendInstance()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("oldTouEntsoe", oldComponent) //
					.activate(null);

			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectSpotTariffProvider(NEW_ENTSOE_SPOT_TARIFF_FACTORY_ID, ENTSOE_TOKEN)));

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldComponent.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldComponent)));

			test.deactivate();
		}

		@Test
		void migrateGridBuyAndSellWithoutTariffProvider() throws Exception {
			final var oldBuy = gridBuyComponent();
			final var oldSell = gridSellComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.NoBackendInstance()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("oldTouEntsoe", oldBuy) //
					.addReference("oldEntsoeGridSell", oldSell) //
					.activate(null);

			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectSpotTariffProvider(NEW_ENTSOE_SPOT_TARIFF_FACTORY_ID, ENTSOE_TOKEN)));

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldBuy.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldBuy)));

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldSell.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_SELL_FACTORY_ID, oldSell)));

			test.deactivate();
		}
	}

	@Nested
	class WithoutAppWithBackend {

		@Test
		void migrateGridBuyWithTariffProvider() throws Exception {
			final var oldComponent = gridBuyComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.Connected()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("oldTouEntsoe", oldComponent) //
					.addReference("spotTariffProvider", mock(SpotTariffProvider.class)) //
					.activate(null);

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldComponent.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldComponent)));

			test.deactivate();
		}

		@Test
		void dontMigrateWithOutdatedBackend() throws Exception {
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.Connected()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("syncableRepository", syncableRepositoryWithoutTariffData()) //
					.addReference("oldTouEntsoe", gridBuyComponent()) //
					.addReference("oldEntsoeGridSell", gridSellComponent()) //
					.activate(null);

			verify(componentManager, never()).handleDeleteComponentConfigRequest(any(), any());
			verify(componentManager, never()).handleCreateComponentConfigRequest(any(), any());

			test.deactivate();
		}

		@Test
		void dontMigrateWithoutBackendConnection() throws Exception {
			final var componentManager = mock(ComponentManager.class);

			createSut(componentManager, new BackendConnectionState.NoConnection(Duration.ofMinutes(1))) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("syncableRepository", syncableRepositoryWithoutTariffData()) //
					.addReference("oldTouEntsoe", gridBuyComponent()) //
					.addReference("oldEntsoeGridSell", gridSellComponent()) //
					.activate(null) //
					.deactivate();

			createSut(componentManager, new BackendConnectionState.StillInitializing()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("syncableRepository", syncableRepositoryWithoutTariffData()) //
					.addReference("oldTouEntsoe", gridBuyComponent()) //
					.addReference("oldEntsoeGridSell", gridSellComponent()) //
					.activate(null) //
					.deactivate();

			verify(componentManager, never()).handleDeleteComponentConfigRequest(any(), any());
			verify(componentManager, never()).handleCreateComponentConfigRequest(any(), any());
		}

		@Test
		void migrateGridBuyAndSellWithWorkingBackendPrices() throws Exception {
			final var oldBuy = gridBuyComponent();
			final var oldSell = gridSellComponent();
			final var componentManager = mock(ComponentManager.class);

			final var test = createSut(componentManager, new BackendConnectionState.Connected()) //
					.addReference("appManagerUtil", appManager()) //
					.addReference("syncableRepository", syncableRepositoryWithTariffData()) //
					.addReference("oldTouEntsoe", oldBuy) //
					.addReference("oldEntsoeGridSell", oldSell) //
					.activate(null);

			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectSpotTariffProvider(NEW_BACKEND_SPOT_TARIFF_FACTORY_ID, null)));

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldBuy.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_BUY_FACTORY_ID, oldBuy)));

			verify(componentManager, times(1)).handleDeleteComponentConfigRequest(any(),
					eq(new DeleteComponentConfig.Request(oldSell.id())));
			verify(componentManager, times(1)).handleCreateComponentConfigRequest(any(),
					argThat(expectComponent(NEW_GRID_SELL_FACTORY_ID, oldSell)));

			test.deactivate();
		}
	}

	static class Matchers {
		private static boolean compareProperties(DummyOldComponent oldComponent,
				List<UpdateComponentConfigRequest.Property> newProperties, Description mismatchDescription) {
			final var oldProperties = oldComponent.getComponentContext().getProperties();
			final var gson = new GsonBuilder().create();
			for (var key : PROPERTIES_TO_COMPARE) {
				var oldValue = switch (key) {
				case "id" -> oldComponent.id();
				case "alias" -> oldComponent.alias();
				case "enabled" -> oldComponent.isEnabled();
				default -> oldProperties.get(key);
				};
				var newValue = newProperties.stream().filter(p -> p.name().equals(key)) //
						.findAny() //
						.map(UpdateComponentConfigRequest.Property::value) //
						.orElse(JsonNull.INSTANCE);

				if (!gson.toJsonTree(oldValue).equals(newValue)) {
					mismatchDescription.appendText("Property '" + key + "' does not match. Expected: "
							+ gson.toJsonTree(oldValue) + ", but was: " + newValue);
					return false;
				}
			}
			return true;
		}

		static Matcher<CreateComponentConfig.Request> expectComponent(String factoryId,
				DummyOldComponent oldComponent) {
			return new TypeSafeMatcher<>() {
				@Override
				public void describeTo(Description description) {
					description.appendText("CreateComponentConfig.Request with same properties as old component");
				}

				private boolean compare(CreateComponentConfig.Request request, Description mismatchDescription) {
					if (!factoryId.equals(request.factoryPid())) {
						mismatchDescription.appendText("Factory PID does not match. Expected: " + factoryId
								+ ", but was: " + request.factoryPid());
						return false;
					}

					return compareProperties(oldComponent, request.properties(), mismatchDescription);
				}

				@Override
				protected boolean matchesSafely(CreateComponentConfig.Request request) {
					return this.compare(request, Description.NONE);
				}

				@Override
				protected void describeMismatchSafely(CreateComponentConfig.Request request,
						Description mismatchDescription) {
					this.compare(request, mismatchDescription);
				}
			};
		}

		static Matcher<CreateComponentConfig.Request> expectSpotTariffProvider(String factoryId, String securityToken) {
			return new TypeSafeMatcher<>() {
				@Override
				public void describeTo(Description description) {
					description.appendText("CreateComponentConfig.Request with expected SpotTariffProvider");
				}

				private boolean compare(CreateComponentConfig.Request request, Description mismatchDescription) {
					if (!factoryId.equals(request.factoryPid())) {
						mismatchDescription.appendText("Factory PID does not match. Expected: " + factoryId
								+ ", but was: " + request.factoryPid());
						return false;
					}

					if (securityToken != null) {
						final var foundSecurityToken = request.properties().stream() //
								.filter(x -> x.name().equals("securityToken")) //
								.findFirst() //
								.map(x -> x.value().getAsString()) //
								.orElse(null);

						if (!securityToken.equals(foundSecurityToken)) {
							mismatchDescription.appendText("Security token does not match. Expected: " + securityToken
									+ ", but was: " + foundSecurityToken);
							return false;
						}
					}

					return true;
				}

				@Override
				protected boolean matchesSafely(CreateComponentConfig.Request request) {
					return this.compare(request, Description.NONE);
				}

				@Override
				protected void describeMismatchSafely(CreateComponentConfig.Request request,
						Description mismatchDescription) {
					this.compare(request, mismatchDescription);
				}
			};
		}
	}

	static class Components {
		static OsgiComponentTest<MyLegacyEntsoeComponentMigrator> createSut(ComponentManager componentManager,
				BackendConnectionState connectionState) throws Exception {
			final var migrator = new MyLegacyEntsoeComponentMigrator();

			final var backendConnectionStateListener = mock(BackendConnectionStateListener.class);
			when(backendConnectionStateListener.getConnectionState())
					.thenReturn(new BehaviorSubject<>(connectionState));

			return new OsgiComponentTest<>(migrator) //
					.addReference("componentManager", componentManager) //
					.addReference("syncableRepository", mock(SyncableRepository.class)) //
					.addReference("backendConnectionStateListener", backendConnectionStateListener);
		}

		static DummyOldComponent gridBuyComponent() {
			return new DummyOldComponent("timeOfUseTariff0", new DummyComponentContext() //
					.addProperty("securityToken", ENTSOE_TOKEN) //
					.addProperty("biddingZone", "GERMANY") //
					.addProperty("ancillaryCosts", "0.02") //
					.addProperty("calculateExpression", "x"));
		}

		static DummyOldComponent gridSellComponent() {
			return new DummyOldComponent("tariffGridSell0", new DummyComponentContext() //
					.addProperty("securityToken", ENTSOE_TOKEN) //
					.addProperty("biddingZone", "GERMANY") //
					.addProperty("calculateExpression", "y"));
		}

		static SyncableRepository syncableRepositoryWithTariffData() {
			final var clock = createDummyClock();
			final var dummySpotTariffProvider = new DummySpotTariffProvider();
			final var tariffData = dummySpotTariffProvider.setRandomTariffData(BiddingZone.GERMANY,
					TimeSpan.between(clock.instant(), clock.instant().plus(1, ChronoUnit.HOURS)),
					Currency.EUR.getJavaCurrency());

			final var syncableRepository = new DummySyncableRepository();
			syncableRepository.setEntry(new SyncableKey.SpotTariff(BiddingZone.GERMANY),
					new SyncableEntry<>(new SyncableKey.SpotTariff(BiddingZone.GERMANY), tariffData, clock.instant()));

			return syncableRepository;
		}

		static SyncableRepository syncableRepositoryWithoutTariffData() {
			final var clock = createDummyClock();
			final var syncableRepository = new DummySyncableRepository();
			syncableRepository.setEntry(new SyncableKey.SpotTariff(BiddingZone.GERMANY),
					new SyncableEntry<>(new SyncableKey.SpotTariff(BiddingZone.GERMANY), null, clock.instant()));

			return syncableRepository;
		}

		static AppManagerUtil appManager(String... appIds) {
			final var appManager = mock(AppManagerUtil.class);
			final var appIdsSet = Set.of(appIds);

			when(appManager.getInstantiatedAppsOfApp(any())).then(invocationOnMock -> {
				var appId = invocationOnMock.getArgument(0, String.class);
				if (appIdsSet.contains(appId)) {
					return List.of(new OpenemsAppInstance(appId, "Some alias", randomUUID(), null, null));
				}
				return List.of();
			});

			return appManager;
		}
	}

	private static class DummyOldComponent extends AbstractDummyOpenemsComponent<DummyOldComponent>
			implements OpenemsComponent {

		DummyOldComponent(String id, DummyComponentContext context) {
			super(id, "Alias " + id, context, OpenemsComponent.ChannelId.values());
		}

		@Override
		protected DummyOldComponent self() {
			return this;
		}
	}

	private static class MyLegacyEntsoeComponentMigrator extends LegacyEntsoeComponentMigrator {

		@Activate
		@Override
		protected void activate() {
			super.activate();
		}

		@Deactivate
		@Override
		protected void deactivate() throws InterruptedException {
			super.deactivate();
		}

		@Override
		protected <T extends OpenemsComponent> void execute(String name, T component,
				ThrowingConsumer<T, Exception> call) {
			try {
				call.accept(component);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
