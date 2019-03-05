package io.openems.common.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedMapDifference;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfigDiff.ComponentDiff.OldNewProperty;

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
					result.add(onlyOnNew.getKey(), onlyOnNew.getValue(), newEntry.getKey(), JsonNull.INSTANCE,
							newEntry.getValue());
				}
			}
		}

		/*
		 * diff deleted Components
		 */
		if (!diffComponents.entriesOnlyOnRight().isEmpty()) {
			for (Entry<String, EdgeConfig.Component> onlyOnOld : diffComponents.entriesOnlyOnRight().entrySet()) {
				for (Entry<String, JsonElement> oldEntry : onlyOnOld.getValue().getProperties().entrySet()) {
					result.add(onlyOnOld.getKey(), onlyOnOld.getValue(), oldEntry.getKey(), oldEntry.getValue(),
							JsonNull.INSTANCE);
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
						result.add(differingComponent.getKey(), newComponent, newEntry.getKey(), JsonNull.INSTANCE,
								newEntry.getValue());
					}
				}
				if (!diffProperties.entriesOnlyOnRight().isEmpty()) {
					// deleted
					for (Entry<String, JsonElement> oldEntry : diffProperties.entriesOnlyOnRight().entrySet()) {
						result.add(differingComponent.getKey(), newComponent, oldEntry.getKey(), oldEntry.getValue(),
								JsonNull.INSTANCE);
					}
				}
				// updated
				if (!diffProperties.entriesDiffering().isEmpty()) {
					for (Entry<String, ValueDifference<JsonElement>> updatedEntry : diffProperties.entriesDiffering()
							.entrySet()) {
						result.add(differingComponent.getKey(), newComponent, updatedEntry.getKey(),
								updatedEntry.getValue().rightValue(), updatedEntry.getValue().leftValue());
					}
				}
			}
		}
		return result;
	}

	protected static class ComponentDiff {
		protected static class OldNewProperty {
			private final JsonElement oldP;
			private final JsonElement newP;

			public OldNewProperty(JsonElement oldP, JsonElement newP) {
				this.oldP = oldP;
				this.newP = newP;
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
		protected Map<String, OldNewProperty> properties = new HashMap<>();

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

	private void add(String componentId, Component component, String propertyName, JsonElement oldValue,
			JsonElement newValue) {
		ComponentDiff.OldNewProperty oldNewProperty = new ComponentDiff.OldNewProperty(oldValue, newValue);
		if (this.components.containsKey(componentId)) {
			this.components.get(componentId).add(propertyName, oldNewProperty);
		} else {
			this.components.put(componentId, new ComponentDiff(component).add(propertyName, oldNewProperty));
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
				String oldP = property.oldP.isJsonNull() ? "" : property.oldP.toString();
				String newP = property.newP.isJsonNull() ? "" : property.newP.toString();

				if (!isFirstProperty) {
					b.append("<tr>");
				}
				b.append(String.format("<td>%s</td><td>%s</td><td>%s</td></tr>", propertyName, oldP, newP));
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
