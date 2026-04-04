package io.openems.core.referencetarget;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

public record PropertyFilter(String property, StringWithParams targetTemplate) {

	/**
	 * Parses {@link PropertyFilter PropertyFilters} from {@link String Strings}.
	 * 
	 * @param propertyTargets the {@link String Strings} to parse
	 * @return the parsed properties
	 */
	public static List<PropertyFilter> fromGenerateTargets(String[] propertyTargets) {
		if (propertyTargets == null) {
			return Collections.emptyList();
		}

		return Arrays.stream(propertyTargets) //
				.map(t -> t.split("=", 2)) //
				.filter(t -> t.length == 2) //
				.map(t -> new PropertyFilter(t[0], new StringWithParams(t[1]))) //
				.toList();
	}

	/**
	 * Parses {@link PropertyFilter PropertyFilters} from the targets of a
	 * reference.
	 * 
	 * @param dto                           the {@link ComponentDescriptionDTO}
	 * @param propertyTargetsFromReferences the names of the references
	 *                                      {@link io.openems.common.referencetarget.GenerateTargetsFromReferences}
	 * @return the parsed properties
	 */
	public static List<PropertyFilter> fromGenerateTargetsFromReferences(ComponentDescriptionDTO dto,
			String[] propertyTargetsFromReferences) {
		if (propertyTargetsFromReferences == null) {
			return Collections.emptyList();
		}

		var stream = Arrays.stream(dto.references);

		if (propertyTargetsFromReferences.length != 0) {
			final var properties = Set.of(propertyTargetsFromReferences);

			stream = stream.filter(t -> properties.contains(t.name));
		}

		return stream.map(t -> new PropertyFilter(t.name, new StringWithParams(t.target))) //
				.toList();
	}

}