package io.openems.common.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.SortedMapDifference;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.types.EdgeConfigDiff.ComponentDiff.Change;
import io.openems.common.types.EdgeConfigDiff.ComponentDiff.OldNewProperty;
import io.openems.common.utils.StringUtils;

public class EdgeConfigDiff {

	/**
	 * Find difference between two EdgeConfigs.
	 *
	 * @param newConfig the new EdgeConfig
	 * @param oldConfig the old EdgeConfig
	 * @return an instance of EdgeConfigDiff
	 */
	public static EdgeConfigDiff diff(EdgeConfig newConfig, EdgeConfig oldConfig) {
		var result = new EdgeConfigDiff();
		SortedMapDifference<String, EdgeConfig.Component> diffComponents = Maps.difference(newConfig.getComponents(),
				oldConfig.getComponents());
		/*
		 * newly created Components
		 */
		if (!diffComponents.entriesOnlyOnLeft().isEmpty()) {
			for (Entry<String, EdgeConfig.Component> onlyOnNew : diffComponents.entriesOnlyOnLeft().entrySet()) {
				result.addCreated(onlyOnNew.getKey(), onlyOnNew.getValue(), onlyOnNew.getValue().getProperties());
			}
		}

		/*
		 * diff deleted Components
		 */
		if (!diffComponents.entriesOnlyOnRight().isEmpty()) {
			for (Entry<String, EdgeConfig.Component> onlyOnOld : diffComponents.entriesOnlyOnRight().entrySet()) {
				result.addDeleted(onlyOnOld.getKey(), onlyOnOld.getValue(), onlyOnOld.getValue().getProperties());
			}
		}

		/*
		 * diff updated Components
		 */
		if (!diffComponents.entriesDiffering().isEmpty()) {
			for (Entry<String, ValueDifference<EdgeConfig.Component>> differingComponent : diffComponents
					.entriesDiffering().entrySet()) {
				var newComponent = differingComponent.getValue().leftValue();
				var oldComponent = differingComponent.getValue().rightValue();

				MapDifference<String, JsonElement> diffProperties = Maps.difference(newComponent.getProperties(),
						oldComponent.getProperties());

				if (diffProperties.areEqual()) {
					// properties are equal -> break early
					continue;
				}

				// ignoring deleted properties from diffProperties.entriesOnlyOnRight(). This
				// can happen with optional properties.

				Map<String, OldNewProperty> properties = new HashMap<>();
				if (!diffProperties.entriesOnlyOnLeft().isEmpty()) {
					// created
					for (Entry<String, JsonElement> newEntry : diffProperties.entriesOnlyOnLeft().entrySet()) {
						if (newEntry.getValue() != null && newEntry.getValue() != JsonNull.INSTANCE) {
							properties.put(newEntry.getKey(),
									new OldNewProperty(JsonNull.INSTANCE, newEntry.getValue()));
						}
					}
				}
				if (!diffProperties.entriesDiffering().isEmpty()) {
					// updated
					for (Entry<String, ValueDifference<JsonElement>> updatedEntry : diffProperties.entriesDiffering()
							.entrySet()) {
						properties.put(updatedEntry.getKey(), new OldNewProperty(updatedEntry.getValue().rightValue(),
								updatedEntry.getValue().leftValue()));
					}
				}
				if (!properties.isEmpty()) {
					result.addUpdated(differingComponent.getKey(), newComponent, properties);
				}
			}
		}
		return result;
	}

	/**
	 * Represents the difference between an old and a new configuration of a
	 * Component.
	 */
	public static class ComponentDiff {
		public static enum Change {
			CREATED("Created"), //
			DELETED("Deleted"), //
			UPDATED("Updated");

			private final String name;

			private Change(String name) {
				this.name = name;
			}

			@Override
			public String toString() {
				return this.name;
			}
		}

		public static class OldNewProperty {
			private final JsonElement oldP;
			private final JsonElement newP;

			public OldNewProperty(JsonElement oldP, JsonElement newP) {
				this.oldP = oldP;
				this.newP = newP;
			}

			public JsonElement getOld() {
				return this.oldP;
			}

			public JsonElement getNew() {
				return this.newP;
			}

			@Override
			public String toString() {
				return "[old=" + this.oldP + ", new=" + this.newP + "]";
			}
		}

		private final Component component;
		private final Change change;
		private OldNewProperty lastChangeBy;
		private OldNewProperty lastChangeAt;
		protected TreeMap<String, OldNewProperty> properties = new TreeMap<>();

		public ComponentDiff(Component component, Change change, OldNewProperty lastChangeBy,
				OldNewProperty lastChangeAt) {
			this.component = component;
			this.change = change;
			this.lastChangeBy = lastChangeBy;
			this.lastChangeAt = lastChangeAt;
		}

		protected ComponentDiff add(String name, OldNewProperty property) {
			switch (name) {
			case OpenemsConstants.PROPERTY_LAST_CHANGE_BY -> this.lastChangeBy = property;

			case OpenemsConstants.PROPERTY_LAST_CHANGE_AT -> this.lastChangeAt = property;

			default -> this.properties.put(name, property);

			}
			return this;
		}

		@Override
		public String toString() {
			return "[" + this.change.toString() + " " + this.component.getFactoryId() + ": properties="
					+ this.properties + "]";
		}

		public Component getComponent() {
			return this.component;
		}

		public TreeMap<String, OldNewProperty> getProperties() {
			return this.properties;
		}
	}

	private final TreeMap<String, ComponentDiff> components = new TreeMap<>();

	/**
	 * Add a newly created component configuration.
	 *
	 * @param componentId the Component-ID of the Property
	 * @param component   the Component instance of the Property
	 * @param properties  the properties
	 */
	private void addCreated(String componentId, Component component, Map<String, JsonElement> properties) {
		var lastChangeBy = new ComponentDiff.OldNewProperty(JsonNull.INSTANCE,
				component.getProperty(OpenemsConstants.PROPERTY_LAST_CHANGE_BY)
						.orElse(new JsonPrimitive("Apache Felix Webconsole")));
		var lastChangeAt = new ComponentDiff.OldNewProperty(JsonNull.INSTANCE,
				component.getProperty(OpenemsConstants.PROPERTY_LAST_CHANGE_AT).orElse(JsonNull.INSTANCE));
		var diff = new ComponentDiff(component, Change.CREATED, lastChangeBy, lastChangeAt);
		for (Entry<String, JsonElement> entry : properties.entrySet()) {
			diff.add(entry.getKey(), new ComponentDiff.OldNewProperty(JsonNull.INSTANCE, entry.getValue()));
		}
		this.components.put(componentId, diff);
	}

	/**
	 * Add a deleted component configuration.
	 *
	 * @param componentId the Component-ID of the Property
	 * @param component   the Component instance of the Property
	 * @param properties  the properties
	 */
	private void addDeleted(String componentId, Component component, Map<String, JsonElement> properties) {
		var lastChangeBy = new ComponentDiff.OldNewProperty(
				component.getProperty(OpenemsConstants.PROPERTY_LAST_CHANGE_BY)
						.orElse(new JsonPrimitive("Apache Felix Webconsole")),
				JsonNull.INSTANCE);
		var lastChangeAt = new ComponentDiff.OldNewProperty(
				component.getProperty(OpenemsConstants.PROPERTY_LAST_CHANGE_AT).orElse(JsonNull.INSTANCE),
				JsonNull.INSTANCE);
		var diff = new ComponentDiff(component, Change.DELETED, lastChangeBy, lastChangeAt);
		for (Entry<String, JsonElement> entry : properties.entrySet()) {
			diff.add(entry.getKey(), new ComponentDiff.OldNewProperty(entry.getValue(), JsonNull.INSTANCE));
		}
		this.components.put(componentId, diff);
	}

	/**
	 * Add an updated component configuration.
	 *
	 * @param componentId the Component-ID of the Property
	 * @param component   the Component instance of the Property
	 * @param properties  the properties
	 */
	private void addUpdated(String componentId, Component component, Map<String, OldNewProperty> properties) {
		var lastChangeBy = new ComponentDiff.OldNewProperty(JsonNull.INSTANCE,
				component.getProperty(OpenemsConstants.PROPERTY_LAST_CHANGE_BY)
						.orElse(new JsonPrimitive("Apache Felix Webconsole")));
		var lastChangeAt = new ComponentDiff.OldNewProperty(JsonNull.INSTANCE, JsonNull.INSTANCE);
		var diff = new ComponentDiff(component, Change.UPDATED, lastChangeBy, lastChangeAt);
		for (Entry<String, OldNewProperty> property : properties.entrySet()) {
			diff.add(property.getKey(), property.getValue());
		}
		this.components.put(componentId, diff);
	}

	/**
	 * Formats the Diff as a HTML table.
	 *
	 * @return a String with the HTML code
	 */
	public String getAsHtml() {
		var b = new StringBuilder();
		b.append("""
				<table border="1" style="border-collapse: collapse"\
					<thead>\
						<tr>\
							<th>Change</th>\
							<th>Component</th>\
							<th>Name</th>\
							<th>Old Value</th>\
							<th>New Value</th>\
						</tr>\
					</thead>\
					<tbody>""");
		for (Entry<String, ComponentDiff> componentEntry : this.components.entrySet()) {
			var componentId = componentEntry.getKey();
			var component = componentEntry.getValue();
			b.append("<tr>");
			// Change column
			b.append(String.format("<td rowspan=\"%s\" style=\"vertical-align: top\">%s</td>",
					component.properties.size() + 2, component.change.toString()));
			// Component-ID + Factory-PID column
			b.append(String.format("<td rowspan=\"%s\" style=\"vertical-align: top\">",
					component.properties.size() + 2));
			b.append(componentId);
			if (!component.component.getFactoryId().isEmpty()) {
				b.append(String.format("<br/>(%s)", component.component.getFactoryId()));
			}
			b.append("</td>");

			// LastChangeBy
			{
				var propertyName = OpenemsConstants.PROPERTY_LAST_CHANGE_BY;
				var property = component.lastChangeBy;
				var oldP = property.oldP.isJsonNull() ? "" : property.oldP.toString();
				var newP = property.newP.isJsonNull() ? "" : property.newP.toString();
				b.append(String.format("<td>%s</td><td>%s</td><td>%s</td></tr>", propertyName, oldP, newP)); // no <tr>!
			}
			// LastChangeAt
			{
				var propertyName = OpenemsConstants.PROPERTY_LAST_CHANGE_AT;
				var property = component.lastChangeAt;
				var oldP = property.oldP.isJsonNull() ? "" : property.oldP.toString();
				var newP = property.newP.isJsonNull() ? "" : property.newP.toString();
				b.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>", propertyName, oldP, newP));
			}
			// Properties
			for (Entry<String, OldNewProperty> propertyEntry : component.properties.entrySet()) {
				var propertyName = propertyEntry.getKey();
				var property = propertyEntry.getValue();
				var oldP = property.oldP.isJsonNull() ? "" : property.oldP.toString();
				var newP = property.newP.isJsonNull() ? "" : property.newP.toString();

				b.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td></tr>", propertyName, oldP, newP));
			}
		}

		b.append("</tbody></table>");
		return b.toString();
	}

	/**
	 * Formats the Diff as Text.
	 *
	 * @return a String representing the Diff
	 */
	public String getAsText() {
		var b = new StringBuilder();
		for (Entry<String, ComponentDiff> componentEntry : this.components.entrySet()) {
			final var component = componentEntry.getValue();
			var change = component.properties.entrySet().stream() //
					.filter(e -> {
						return switch (e.getKey()) {
						case "_lastChangeAt", "_lastChangeBy", "org.ops4j.pax.logging.appender.name" -> // ignore
							false;

						default -> true;

						};
					}) //
					.map(e -> {
						String oldValue = StringUtils.toShortString(e.getValue().getOld(), 20);
						String newValue = StringUtils.toShortString(e.getValue().getNew(), 20);

						return switch (component.change) {
						case CREATED -> e.getKey() + "=" + newValue;
						case UPDATED -> e.getKey() + "=" + newValue + " [was:" + oldValue + "]";
						case DELETED -> e.getKey() + " [was:" + oldValue + "]";
						default -> {
							// can never happen
							assert true;
							yield "";
						}
						};
					}).collect(Collectors.joining(", "));
			if (change.isEmpty()) {
				continue;
			}

			b.append(component.change);
			b.append(" ");
			b.append(componentEntry.getKey());
			var factoryId = component.component.getFactoryId();
			if (!factoryId.isEmpty()) {
				b.append(String.format(" (%s)", factoryId));
			}
			b.append(": ");
			b.append(change);
			b.append("\n");
		}
		return b.toString();
	}

	public TreeMap<String, ComponentDiff> getComponents() {
		return this.components;
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
		return this.getAsText().replace("\n", "; ");
	}
}
