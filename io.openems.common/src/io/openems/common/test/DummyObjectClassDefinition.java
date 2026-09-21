package io.openems.common.test;

import java.io.InputStream;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public record DummyObjectClassDefinition(//
		String name, //
		String id, //
		String description, //
		AttributeDefinition[] attributes //
) implements ObjectClassDefinition {

	public static final DummyObjectClassDefinition EMPTY = new DummyObjectClassDefinition(null, null, null,
			new AttributeDefinition[0]);

	/**
	 * Creates a new {@link DummyObjectClassDefinition} with the specific name.
	 * 
	 * @param name the new name
	 * @return a new {@link DummyObjectClassDefinition} with the specific name
	 */
	public DummyObjectClassDefinition withName(String name) {
		return new DummyObjectClassDefinition(name, this.id, this.description, this.attributes);
	}

	/**
	 * Creates a new {@link DummyObjectClassDefinition} with the specific id.
	 *
	 * @param id the new id
	 * @return a new {@link DummyObjectClassDefinition} with the specific id
	 */
	public DummyObjectClassDefinition withId(String id) {
		return new DummyObjectClassDefinition(this.name, id, this.description, this.attributes);
	}

	/**
	 * Creates a new {@link DummyObjectClassDefinition} with the specific
	 * description.
	 *
	 * @param description the new description
	 * @return a new {@link DummyObjectClassDefinition} with the specific
	 *         description
	 */
	public DummyObjectClassDefinition withDescription(String description) {
		return new DummyObjectClassDefinition(this.name, this.id, description, this.attributes);
	}

	/**
	 * Creates a new {@link DummyObjectClassDefinition} with the specific
	 * attributes.
	 *
	 * @param attributes the new attributes
	 * @return a new {@link DummyObjectClassDefinition} with the specific attributes
	 */
	public DummyObjectClassDefinition withAttributes(AttributeDefinition... attributes) {
		return new DummyObjectClassDefinition(this.name, this.id, this.description, attributes);
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getID() {
		return this.id;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public AttributeDefinition[] getAttributeDefinitions(int i) {
		return this.attributes;
	}

	@Override
	public InputStream getIcon(int i) {
		throw new UnsupportedOperationException();
	}

}
