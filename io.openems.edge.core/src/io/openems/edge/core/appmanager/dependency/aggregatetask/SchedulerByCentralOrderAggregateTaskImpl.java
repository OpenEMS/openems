package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;

@Component(//
		service = { //
				AggregateTask.class, //
				SchedulerByCentralOrderAggregateTask.class, //
				SchedulerByCentralOrderAggregateTaskImpl.class //
		}, //
		scope = ServiceScope.SINGLETON //
)
public class SchedulerByCentralOrderAggregateTaskImpl implements SchedulerByCentralOrderAggregateTask {

	private final ComponentManager componentManager;
	private final ComponentUtil componentUtil;
	private final AppManagerUtil appManagerUtil;
	private final ComponentAggregateTask aggregateTask;

	private final SchedulerOrderDefinition order;

	private List<SchedulerComponent> schedulerComponents;
	private List<SchedulerComponent> removeSchedulerComponents;

	@Component(service = SchedulerOrderDefinition.class)
	public static final class ProductionSchedulerOrderDefinition extends SchedulerOrderDefinition {

		public ProductionSchedulerOrderDefinition() {
			this.thenByFactoryId("Controller.Ess.PrepareBatteryExtension") //
					.thenByFactoryId("Controller.Ess.FixActivePower") //
					.thenByFactoryId("Controller.Ess.EmergencyCapacityReserve") //
					.thenBy(new SchedulerOrderDefinition() //
							.filterByFactoryId("Controller.Api.ModbusTcp.ReadWrite") //
							.thenByCreatedAppId("App.Ess.GeneratingPlantController") //
							.rest()) //
					.thenByFactoryId("Controller.Api.Rest.ReadWrite") //
					.thenByFactoryId("Controller.Ess.GridOptimizedCharge") //
					.thenByFactoryId("Controller.Ess.Hybrid.Surplus-Feed-To-Grid") //
					.thenByFactoryId("Controller.Evcs") //
					.thenByFactoryId("Controller.Ess.Time-Of-Use-Tariff") //
					.thenByFactoryId("Controller.Symmetric.Balancing") //
			;
		}

	}

	@Activate
	public SchedulerByCentralOrderAggregateTaskImpl(//
			@Reference ComponentManager componentManager, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil, //
			@Reference ComponentAggregateTask aggregateTask, //
			@Reference SchedulerOrderDefinition schedulerOrderDefinition //
	) {
		this.componentManager = componentManager;
		this.componentUtil = componentUtil;
		this.aggregateTask = aggregateTask;
		this.appManagerUtil = appManagerUtil;
		this.order = schedulerOrderDefinition;
	}

	public static class SchedulerOrderDefinition implements Comparator<SchedulerComponent> {

		private static final int[] EMPTY_INT_ARRAY = new int[0];

		private static sealed class PositionReturnType {

			public static final PositionReturnType right(int... index) {
				return new Right(index);
			}

			public static final PositionReturnType remove() {
				return Remove.INSTANCE;
			}

			public static final PositionReturnType next() {
				return Next.INSTANCE;
			}

			public static final class Right extends PositionReturnType {
				public final int[] index;

				public Right(int[] index) {
					this.index = index;
				}

			}

			private static final class Remove extends PositionReturnType {

				public static final Remove INSTANCE = new Remove();

			}

			private static final class Next extends PositionReturnType {

				public static final Next INSTANCE = new Next();

			}

		}

		private final List<Function<SchedulerComponent, PositionReturnType>> predicates = new LinkedList<>();

		/**
		 * Adds a filter to the order queue which sorts out every
		 * {@link SchedulerComponent} which does not match the {@link Predicate}.
		 * 
		 * @param matcher the predicate to determine if the current
		 *                {@link SchedulerComponent} should be filtered out or not; true
		 *                for continuing to next check; false for removing the
		 *                {@link SchedulerComponent}
		 * @return this
		 */
		public SchedulerOrderDefinition filterBy(Predicate<SchedulerComponent> matcher) {
			return this.thenByFunction(t -> matcher.test(t) ? PositionReturnType.next() : PositionReturnType.remove());
		}

		/**
		 * Adds a filter to the order queue which sorts out every
		 * {@link SchedulerComponent} which does not match the factoryId.
		 * 
		 * @param factoryId the factoryId to determine if the current
		 *                  {@link SchedulerComponent} should be filtered out or not;
		 *                  same for continuing to next check; different for removing
		 *                  the {@link SchedulerComponent}
		 * @return this
		 */
		public SchedulerOrderDefinition filterByFactoryId(String factoryId) {
			return this.filterBy(t -> factoryId.equals(t.factoryId()));
		}

		private SchedulerOrderDefinition thenByFunction(Function<SchedulerComponent, PositionReturnType> matcher) {
			this.predicates.add(t -> matcher.apply(t));
			return this;
		}

		/**
		 * Adds a matching function if the current {@link SchedulerComponent} is at the
		 * right position.
		 * 
		 * @param matcher the predicate if the current {@link SchedulerComponent} is at
		 *                the right position; true if the {@link SchedulerComponent} is
		 *                at the right position; false if the {@link SchedulerComponent}
		 *                should continue to the next function
		 * @return this
		 */
		public SchedulerOrderDefinition thenBy(Predicate<SchedulerComponent> matcher) {
			return this.thenByFunction(t -> matcher.test(t) ? PositionReturnType.right(1) : PositionReturnType.next());
		}

		/**
		 * Adds a Sub-Order at the current position.
		 * 
		 * @param order the Sub-Order
		 * @return this
		 */
		public SchedulerOrderDefinition thenBy(SchedulerOrderDefinition order) {
			return this.thenByFunction(t -> {
				final var result = order.indexOfMatch(t);
				if (result.length == 0) {
					return PositionReturnType.next();
				}
				return PositionReturnType.right(result);
			});
		}

		/**
		 * Adds a matching function if the current {@link SchedulerComponent} matches
		 * the factoryId.
		 * 
		 * @param factoryId the factoryId if the current {@link SchedulerComponent} has
		 *                  the same factoryId it is inserted at this position;
		 *                  otherwise the {@link SchedulerComponent} continues to the
		 *                  next function should continue to the next function
		 * @return this
		 */
		public SchedulerOrderDefinition thenByFactoryId(String factoryId) {
			return this.thenBy(t -> factoryId.equals(t.factoryId()));
		}

		/**
		 * Adds a matching function if the current {@link SchedulerComponent} matches
		 * the appId. right position.
		 * 
		 * @param appId the appId if the current {@link SchedulerComponent} has the same
		 *              appId it is inserted at this position; otherwise the
		 *              {@link SchedulerComponent} continues to the next function should
		 *              continue to the next function
		 * @return this
		 */
		public SchedulerOrderDefinition thenByCreatedAppId(String appId) {
			return this.thenBy(t -> appId.equals(t.createdByAppId()));
		}

		/**
		 * Adds the rest of the {@link SchedulerComponent} at this position which got
		 * not filtered out before.
		 * 
		 * @return this
		 */
		public SchedulerOrderDefinition rest() {
			return this.thenBy(t -> true);
		}

		@Override
		public int compare(SchedulerComponent o1, SchedulerComponent o2) {
			final var index1 = this.indexOfMatch(o1);
			final var index2 = this.indexOfMatch(o2);

			for (int i = 0; i < Math.max(index1.length, index2.length); i++) {
				if (i >= index1.length) {
					return 1;
				}
				if (i >= index2.length) {
					return -1;
				}
				final var i1Value = index1[i];
				final var i2Value = index2[i];
				if (i1Value < i2Value) {
					return -1;
				}
				if (i1Value > i2Value) {
					return 1;
				}
			}
			return 0;
		}

		/**
		 * Checks if the given {@link SchedulerComponent} is handled by this
		 * {@link SchedulerOrderDefinition}.
		 * 
		 * @param o the {@link SchedulerComponent}
		 * @return true if the {@link SchedulerComponent} is handled by this order
		 */
		public boolean contains(SchedulerComponent o) {
			return this.indexOfMatch(o).length != 0;
		}

		private final int[] indexOfMatch(SchedulerComponent o) {
			final var iterator = this.predicates.listIterator();
			while (iterator.hasNext()) {
				final var index = iterator.nextIndex();
				final var predicate = iterator.next();
				final var result = predicate.apply(o);

				if (result instanceof PositionReturnType.Next) {
					continue;
				}
				if (result instanceof PositionReturnType.Remove) {
					return EMPTY_INT_ARRAY;
				}
				if (result instanceof PositionReturnType.Right right) {
					final var array = new int[right.index.length + 1];
					array[0] = index;
					System.arraycopy(right.index, 0, array, 1, right.index.length);
					return array;
				}
			}
			return EMPTY_INT_ARRAY;
		}

	}

	@Override
	public void reset() {
		this.schedulerComponents = new LinkedList<>();
		this.removeSchedulerComponents = new LinkedList<>();
	}

	@Override
	public void aggregate(//
			final SchedulerByCentralOrderConfiguration currentConfiguration, //
			final SchedulerByCentralOrderConfiguration lastConfiguration //
	) {
		if (currentConfiguration != null) {
			this.schedulerComponents.addAll(currentConfiguration.componentOrder());
		}
		if (lastConfiguration != null) {
			var schedulerIdDiff = new ArrayList<>(lastConfiguration.componentOrder());
			if (currentConfiguration != null) {
				schedulerIdDiff.removeAll(currentConfiguration.componentOrder());
			}
			this.removeSchedulerComponents.addAll(schedulerIdDiff);
		}
	}

	@Override
	public void create(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (!this.anyCreateChanges()) {
			this.delete(user, otherAppConfigurations);
			return;
		}

		final var handledIds = new TreeSet<>(this.order);
		for (final var component : this.schedulerComponents) {
			if (!this.order.contains(component)) {
				throw new OpenemsException("Unhandled component " + component);
			}
			handledIds.add(component);
		}

		for (final var config : otherAppConfigurations) {
			final var schedulerConfig = config.getConfiguration(SchedulerByCentralOrderAggregateTask.class);
			if (schedulerConfig == null) {
				continue;
			}

			for (final var component : schedulerConfig.componentOrder()) {
				if (!this.order.contains(component)) {
					throw new OpenemsException("Unhandled component " + component);
				}
				handledIds.add(component);
			}
		}

		final var existingIds = this.componentUtil.removeIdsWhichNotExist(
				handledIds.stream().map(SchedulerComponent::id).toList(), this.aggregateTask.getCreatedComponents());
		handledIds.removeIf(t -> !existingIds.contains(t.id()));

		final var schedulerIds = this.componentUtil.getSchedulerIds();
		for (final var id : schedulerIds) {
			final var existingComponent = handledIds.stream() //
					.filter(t -> t.id().equals(id)) //
					.findAny() //
					.orElse(null);
			if (existingComponent != null) {
				continue;
			}

			// also include existing components based on their factory id
			try {
				final var component = this.componentManager.getComponent(id);
				final var schedulerComponent = new SchedulerComponent(id, component.serviceFactoryPid(), null);
				if (!this.order.contains(schedulerComponent)) {
					continue;
				}
				if (!handledIds.contains(schedulerComponent)) {
					handledIds.add(schedulerComponent);
				}
			} catch (OpenemsNamedException e) {
				// configured id in scheduler does not exist
			}
		}

		final var finalOrder = new ArrayList<>(schedulerIds);
		for (final var schedulerComponent : handledIds) {
			final var lower = handledIds.lower(schedulerComponent);
			final var higher = handledIds.higher(schedulerComponent);
			final var iLower = lower == null ? -1 : finalOrder.indexOf(lower.id());
			final var i = finalOrder.indexOf(schedulerComponent.id());
			final var iHigher = higher == null ? -1 : finalOrder.indexOf(higher.id());

			if (i != -1 //
					&& (iLower == -1 || iLower < i) //
					&& (iHigher == -1 || iHigher > i)) {
				// already correctly inserted
				continue;
			}

			final var insertIndex = Math.max(iLower + 1, iHigher);
			finalOrder.add(insertIndex, schedulerComponent.id());
			if (i != -1) {
				finalOrder.remove(Math.max(0, insertIndex >= i ? i : (i - 1)));
			}
		}

		final var idsToRemove = this.getIdsToRemove(otherAppConfigurations);
		finalOrder.removeAll(idsToRemove);

		this.componentUtil.setSchedulerComponentIds(user, finalOrder);
	}

	@Override
	public void delete(User user, List<AppConfiguration> otherAppConfigurations) throws OpenemsNamedException {
		if (!this.anyDeleteChanges()) {
			return;
		}

		this.componentUtil.removeIdsInSchedulerIfExisting(user, this.getIdsToRemove(otherAppConfigurations));
	}

	private List<String> getIdsToRemove(List<AppConfiguration> otherAppConfigurations) {
		final var otherIds = AppConfiguration
				.flatMap(otherAppConfigurations, SchedulerByCentralOrderAggregateTask.class,
						SchedulerByCentralOrderConfiguration::componentOrder) //
				.toList();

		this.removeSchedulerComponents.removeIf(t -> otherIds.stream().anyMatch(c -> c.id().equals(t.id())));
		final var idsToRemove = new ArrayList<>(
				this.removeSchedulerComponents.stream().map(SchedulerComponent::id).toList());
		idsToRemove.addAll(this.aggregateTask.getDeletedComponents());

		return idsToRemove;
	}

	@Override
	public String getGeneralFailMessage(Language l) {
		final var bundle = AppManagerAppHelperImpl.getTranslationBundle(l);
		return TranslationUtil.getTranslation(bundle, "canNotUpdateScheduler");
	}

	@Override
	public AggregateTaskExecuteConstraints getExecuteConstraints() {
		return new AggregateTaskExecuteConstraints(Set.of(//
				// Needs to run after the AggregateTask.ComponentAggregateTask to also remove
				// ids in the scheduler of components which got deleted
				ComponentAggregateTask.class //
		));
	}

	@Override
	public void validate(//
			final List<String> errors, //
			final AppConfiguration appConfiguration, //
			final SchedulerByCentralOrderConfiguration configuration //
	) {
		if (configuration.componentOrder().isEmpty()) {
			return;
		}

		List<String> existingOrder;
		try {
			existingOrder = this.componentUtil.getSchedulerIds();
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return;
		}

		final var expectedOrder = new TreeSet<>(this.order);
		for (final var componentId : existingOrder) {
			final var instances = this.getAppsWithSchedulerComponent(componentId);

			if (instances.size() != 1) {
				// not created with app
				continue;
			}
			final var entry = instances.entrySet().iterator().next();
			final var appId = entry.getKey().appId;
			final var schedulerComponents = entry.getValue();
			if (schedulerComponents.isEmpty()) {
				continue;
			}
			if (schedulerComponents.size() > 1) {
				errors.add("Multiple SchedulerComponents found with the same id");
				continue;
			}
			final var schedulerComponent = schedulerComponents.get(0);
			expectedOrder.add(new SchedulerComponent(componentId, schedulerComponent.factoryId(), appId));
		}

		for (final var schedulerComponent : configuration.componentOrder()) {
			final var higher = expectedOrder.higher(schedulerComponent);
			final var lower = expectedOrder.lower(schedulerComponent);
			if (!expectedOrder.add(schedulerComponent)) {
				// check if id was configured between the expected ids
				final var highIndex = higher == null ? -1 : existingOrder.indexOf(higher.id());
				final var index = existingOrder.indexOf(schedulerComponent.id());
				final var lowIndex = lower == null ? -1 : existingOrder.indexOf(lower.id());

				if ((highIndex == -1 || highIndex > index) //
						&& (lowIndex != -1 || lowIndex < index)) {
					continue;
				}
			}

			errors.add("Expected '" //
					+ schedulerComponent.id() //
					+ "' to be configured between '" //
					+ (lower == null ? "TOP" : lower.id()) //
					+ "' and '" //
					+ (higher == null ? "BOTTOM" : higher.id()) //
					+ "'");
		}
	}

	private boolean anyCreateChanges() {
		return !this.schedulerComponents.isEmpty();
	}

	private boolean anyDeleteChanges() {
		return !this.removeSchedulerComponents.isEmpty();
	}

	private Map<OpenemsAppInstance, List<SchedulerComponent>> getAppsWithSchedulerComponent(String componentId) {
		return this.appManagerUtil.getInstantiatedApps().stream() //
				.<Entry<OpenemsAppInstance, SchedulerComponent>>mapMulti((t, c) -> {
					try {
						final var configuration = this.appManagerUtil.getAppConfiguration(ConfigurationTarget.VALIDATE,
								t, Language.DEFAULT);

						final var schedulerConfig = configuration
								.getConfiguration(SchedulerByCentralOrderAggregateTask.class);
						if (schedulerConfig == null) {
							return;
						}
						for (var schedulerComponent : schedulerConfig.componentOrder()) {
							if (schedulerComponent.id().equals(componentId)) {
								c.accept(Map.entry(t, schedulerComponent));
							}
						}
					} catch (OpenemsNamedException e) {
						// can not get app configuration
					}
				}) //
				.collect(groupingBy(Entry::getKey, mapping(Entry::getValue, toList())));
	}

}
