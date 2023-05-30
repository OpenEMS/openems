package io.openems.edge.common.channel;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.CaseFormat;

/**
 * A {@link ChannelId} defines a Channel. It provides a unique Name and and a
 * {@link Doc}.
 *
 * <p>
 * This interface is typically implemented by an {@link Enum} type which
 * automatically provides a {@link ChannelId#name()} method.
 */
public interface ChannelId {

	/**
	 * Converts a Channel-ID in UPPER_UNDERSCORE format (like from an {@link Enum})
	 * to the preferred UPPER_CAMEL format.
	 *
	 * <p>
	 * Examples: converts "ACTIVE_POWER" to "ActivePower".
	 *
	 * <p>
	 * Special reserved Channel-IDs starting with "_" have a special handling:
	 * "_PROPERTY_ENABLED" is converted to "_PropertyEnabled".
	 *
	 * @param name a Channel-ID in UPPER_UNDERSCORE format
	 * @return the Channel-ID in UPPER_CAMEL format.
	 */
	public static String channelIdUpperToCamel(String name) {
		if (name.startsWith("_")) {
			// special handling for reserved Channel-IDs starting with "_".
			return "_" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name.substring(1));
		}
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
	}

	/**
	 * Converts a Channel-ID in UPPER_CAMEL format to the UPPER_UNDERSCORE format.
	 *
	 * <p>
	 * Examples: converts "ActivePower" to "ACTIVE_POWER".
	 *
	 * @param name Channel-ID in UPPER_CAMEL format.
	 * @return the a Channel-ID in UPPER_UNDERSCORE format
	 */
	public static String channelIdCamelToUpper(String name) {
		if (name.startsWith("_")) {
			// special handling for reserved Channel-IDs starting with "_".
			return "_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name.substring(1));
		}
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
	}

	/**
	 * Lists all Channel-IDs of the given Channel-ID Enum in a form that is suitable
	 * for a InfluxDB-Query in a Grafana Dashboard.
	 *
	 * <p>
	 * To create a query, call this function like
	 * `ChannelId.printChannelIdsForInfluxQuery(FeneconMiniEss.ServiceInfoChannelId.values());`
	 * 
	 * @param <T>        the actual type
	 * @param channelIds the {@link ChannelId}s, e.g. from ChannelId.values().
	 */
	public static <T extends Enum<T>> void printChannelIdsForInfluxQuery(ChannelId[] channelIds) {
		System.out.println(Stream.of(channelIds) //
				.map(c -> {
					var name = c.doc().getText();
					if (name.isEmpty()) {
						name = c.id();
					}
					return "mean(\"$ess/" + c.id() + "\") as \"" + name + "\"";
				}).collect(Collectors.joining(", ")));
	}

	/**
	 * Gets the name in format {@link CaseFormat#UPPER_UNDERSCORE}. This is
	 * available by default for an Enum.
	 *
	 * <p>
	 * Names starting with underscore ("_") are reserved for internal usage.
	 *
	 * @return the name
	 */
	String name();

	/**
	 * Gets the name in CamelCase.
	 *
	 * @return the Channel-ID in CamelCase
	 */
	default String id() {
		return channelIdUpperToCamel(this.name());
	}

	/**
	 * Gets the Channel Doc for this ChannelId.
	 *
	 * @return the Channel-Doc
	 */
	Doc doc();
	
	public record ChannelIdImpl(String name, Doc doc) implements ChannelId {
	}
}
