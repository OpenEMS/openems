package io.openems.core.referencetarget;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

public record PropertyFilter(String property, StringWithParams targetTemplate) {

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