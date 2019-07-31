package io.openems.common.types;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedMapDifference;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfigDiff.ComponentDiff.OldNewProperty;
import io.openems.common.types.EdgeConfigDiff.ComponentDiff.OldNewProperty.Change;

public class EdgeConfigDiff {

	/**
	 * Find difference between two EdgeConfigs.
	 * 
	 * @param newConfig the new EdgeConfig
	 * @param oldConfig the old EdgeConfig
	 * @return an instance of EdgeConfigDiff
	 */
	public static EdgeConfigDiff diff(EdgeConfig newConfig, EdgeConfig oldConfig) {
		EdgeConfigDiff result = new EdgeConfigDiff();
		SortedMapDifference<String, EdgeConfig.Component> diffComponents = Maps.difference(newConfig.getComponents(),
				oldConfig.getComponents());
		/*
		 * newly created Components
		 */
		if (!diffComponents.entriesOnlyOnLeft().isEmpty()) {
			for (Entry<String, EdgeConfig.Component> onlyOnNew : diffComponents.entriesOnlyOnLeft().entrySet()) {
				for (Entry<String, JsonElement> newEntry : onlyOnNew.getValue().getProperties().entrySet()) {
					result.add(onlyOnNew.getKey(), onlyOnNew.getValue(), Change.CREATED, newEntry.getKey(),
							JsonNull.INSTANCE, newEntry.getValue());
				}
			}
		}

		/*
		 * diff deleted Components
		 */
		if (!diffComponents.entriesOnlyOnRight().isEmpty()) {
			for (Entry<String, EdgeConfig.Component> onlyOnOld : diffComponents.entriesOnlyOnRight().entrySet()) {
				for (Entry<String, JsonElement> oldEntry : onlyOnOld.getValue().getProperties().entrySet()) {
					result.add(onlyOnOld.getKey(), onlyOnOld.getValue(), Change.DELETED, oldEntry.getKey(),
							oldEntry.getValue(), JsonNull.INSTANCE);
				}
			}
		}

		/*
		 * diff updated Components
		 */
		if (!diffComponents.entriesDiffering().isEmpty()) {
			for (Entry<String, ValueDifference<EdgeConfig.Component>> differingComponent : diffComponents
					.entriesDiffering().entrySet()) {
				EdgeConfig.Component newComponent = differingComponent.getValue().leftValue();
				EdgeConfig.Component oldComponent = differingComponent.getValue().rightValue();

				MapDifference<String, JsonElement> diffProperties = Maps.difference(newComponent.getProperties(),
						oldComponent.getProperties());

				if (diffProperties.areEqual()) {
					// properties are equal -> break early
					continue;
				}

				if (!diffProperties.entriesOnlyOnLeft().isEmpty()) {
					// created
					for (Entry<String, JsonElement> newEntry : diffProperties.entriesOnlyOnLeft().entrySet()) {
						result.add(differingComponent.getKey(), newComponent, Change.CREATED, newEntry.getKey(),
								JsonNull.INSTANCE, newEntry.getValue());
					}
				}
				if (!diffProperties.entriesOnlyOnRight().isEmpty()) {
					// deleted
					for (Entry<String, JsonElement> oldEntry : diffProperties.entriesOnlyOnRight().entrySet()) {
						result.add(differingComponent.getKey(), newComponent, Change.DELETED, oldEntry.getKey(),
								oldEntry.getValue(), JsonNull.INSTANCE);
					}
				}
				// updated
				if (!diffProperties.entriesDiffering().isEmpty()) {
					for (Entry<String, ValueDifference<JsonElement>> updatedEntry : diffProperties.entriesDiffering()
							.entrySet()) {
						result.add(differingComponent.getKey(), newComponent, Change.UPDATED, updatedEntry.getKey(),
								updatedEntry.getValue().rightValue(), updatedEntry.getValue().leftValue());
					}
				}
			}
		}
		return result;
	}

	/**
	 * Represents the difference between an old and a new configuration of a
	 * Component.
	 */
	protected static class ComponentDiff {
		protected static class OldNewProperty {
			protected static enum Change {
				CREATED("Created"), //
				DELETED("Deleted"), //
				UPDATED("Created");

				private final String name;

				private Change(String name) {
					this.name = name;
				}

				@Override
				public String toString() {
					return this.name;
				}
			}

			private final Change change;
			private final JsonElement oldP;
			private final JsonElement newP;

			public OldNewProperty(Change change, JsonElement oldP, JsonElement newP) {
				this.change = change;
				this.oldP = oldP;
				this.newP = newP;
			}

			public Change getChange() {
				return change;
			}

			public JsonElement getOld() {
				return oldP;
			}

			public JsonElement getNew() {
				return newP;
			}

			@Override
			public String toString() {
				return "[old=" + oldP + ", new=" + newP + "]";
			}
		}

		private final Component component;
		protected TreeMap<String, OldNewProperty> properties = new TreeMap<>();

		public ComponentDiff(Component component) {
			this.component = component;
		}

		public ComponentDiff add(String name, OldNewProperty property) {
			this.properties.put(name, property);
			return this;
		}

		@Override
		public String toString() {
			return "[" + component.getFactoryId() + ": properties=" + properties + "]";
		}
	}

	private final TreeMap<String, ComponentDiff> components = new TreeMap<>();

	/**
	 * Add the difference of a Property to the List.
	 * 
	 * @param componentId  the Component-ID of the Property
	 * @param component    the Component instance of the Property
	 * @param change       the type of {@link Change}
	 * @param propertyName the name of the Property
	 * @param oldValue     the old value
	 * @param newValue     the new value
	 */
	private void add(String componentId, Component component, Change change, String propertyName, JsonElement oldValue,
			JsonElement newValue) {
		ComponentDiff.OldNewProperty oldNewProperty = new ComponentDiff.OldNewProperty(change, oldValue, newValue);
		if (this.components.containsKey(componentId)) {
			this.components.get(componentId).add(propertyName, oldNewProperty);
		} else {
			JsonElement lastChangeBy = Optional.ofNullable(//
					component.getProperties().get(OpenemsConstants.PROPERTY_LAST_CHANGE_BY)).orElse(JsonNull.INSTANCE);
			JsonElement lastChangeAt = Optional.ofNullable(//
					component.getProperties().get(OpenemsConstants.PROPERTY_LAST_CHANGE_AT)).orElse(JsonNull.INSTANCE);
			this.components.put(componentId, new ComponentDiff(component) //
					// add some important properties on top first
					.add(OpenemsConstants.PROPERTY_LAST_CHANGE_BY,
							new OldNewProperty(Change.CREATED, JsonNull.INSTANCE, lastChangeBy)) //
					.add(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
							new OldNewProperty(Change.CREATED, JsonNull.INSTANCE, lastChangeAt)) //
					// add this property
					.add(propertyName, oldNewProperty));
		}
	}

	/**
	 * Formats the Diff as a HTML table.
	 * 
	 * @return a String with the HTML code
	 */
	public String getAsHtml() {
		StringBuilder b = new StringBuilder();
		b.append("<table border=\"1\" style=\"border-collapse: collapse\"" + //
				"	<thead>" + //
				"		<tr>" + //
				"			<th>Component</th>" + //
				"			<th>Name</th>" + //
				"			<th>Change</th>" + //
				"			<th>Old Value</th>" + //
				"			<th>New Value</th>" + //
				"		</tr>" + //
				"	</thead>" + //
				"	<tbody>");
		for (Entry<String, ComponentDiff> componentEntry : this.components.entrySet()) {
			String componentId = componentEntry.getKey();
			ComponentDiff component = componentEntry.getValue();
			b.append(String.format("<tr><td rowspan=\"%s\" style=\"vertical-align: top\">%s<br/>(%s)</td>",
					component.properties.size(), componentId, component.component.getFactoryId()));

			boolean isFirstProperty = true;
			for (Entry<String, OldNewProperty> propertyEntry : component.properties.entrySet()) {
				String propertyName = propertyEntry.getKey();
				OldNewProperty property = propertyEntry.getValue();
				Change change = property.getChange();
				String oldP = property.oldP.isJsonNull() ? "" : property.oldP.toString();
				String newP = property.newP.isJsonNull() ? "" : property.newP.toString();

				if (!isFirstProperty) {
					b.append("<tr>");
				}
				b.append(String.format("<td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", propertyName, change, oldP,
						newP));
				isFirstProperty = false;
			}
			b.append("</tr>");
		}

		b.append("</tbody></table>");
		return b.toString();
	}

	/**
	 * Gets whether this diff is not empty, i.e. the EdgeConfig instances were
	 * different.
	 * 
	 * @return true for different EdgeConfigs.
	 */
	public boolean isDifferent() {
		return !this.components.isEmpty();
	}

	@Override
	public String toString() {
		return components.toString();
	}
}
