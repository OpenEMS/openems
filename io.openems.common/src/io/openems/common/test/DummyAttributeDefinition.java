package io.openems.common.test;

import org.osgi.service.metatype.AttributeDefinition;

public record DummyAttributeDefinition(//
		String name, //
		String id, //
		String description, //
		int cardinality, //
		int type, //
		String[] optionValues, //
		String[] optionLabels, //
		String[] defaultValue //
) implements AttributeDefinition {

	public static final DummyAttributeDefinition EMPTY = new DummyAttributeDefinition(null, null, null, 0,
			AttributeDefinition.STRING, new String[0], new String[0], new String[0]);

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific name.
	 *
	 * @param name the new name
	 * @return a new {@link DummyAttributeDefinition} with the specific name
	 */
	public DummyAttributeDefinition withName(String name) {
		return new DummyAttributeDefinition(name, this.id, this.description, this.cardinality, this.type,
				this.optionValues, this.optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific id.
	 *
	 * @param id the new id
	 * @return a new {@link DummyAttributeDefinition} with the specific id
	 */
	public DummyAttributeDefinition withId(String id) {
		return new DummyAttributeDefinition(this.name, id, this.description, this.cardinality, this.type,
				this.optionValues, this.optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific description.
	 *
	 * @param description the new description
	 * @return a new {@link DummyAttributeDefinition} with the specific description
	 */
	public DummyAttributeDefinition withDescription(String description) {
		return new DummyAttributeDefinition(this.name, this.id, description, this.cardinality, this.type,
				this.optionValues, this.optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific cardinality.
	 *
	 * @param cardinality the new cardinality
	 * @return a new {@link DummyAttributeDefinition} with the specific cardinality
	 */
	public DummyAttributeDefinition withCardinality(int cardinality) {
		return new DummyAttributeDefinition(this.name, this.id, this.description, cardinality, this.type,
				this.optionValues, this.optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific type.
	 *
	 * @param type the new type
	 * @return a new {@link DummyAttributeDefinition} with the specific type
	 */
	public DummyAttributeDefinition withType(int type) {
		return new DummyAttributeDefinition(this.name, this.id, this.description, this.cardinality, type,
				this.optionValues, this.optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific option
	 * values.
	 *
	 * @param optionValues the new option values
	 * @return a new {@link DummyAttributeDefinition} with the specific option
	 *         values
	 */
	public DummyAttributeDefinition withOptionValues(String... optionValues) {
		return new DummyAttributeDefinition(this.name, this.id, this.description, this.cardinality, this.type,
				optionValues, this.optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific option
	 * labels.
	 *
	 * @param optionLabels the new option labels
	 * @return a new {@link DummyAttributeDefinition} with the specific option
	 *         labels
	 */
	public DummyAttributeDefinition withOptionLabels(String... optionLabels) {
		return new DummyAttributeDefinition(this.name, this.id, this.description, this.cardinality, this.type,
				this.optionValues, optionLabels, this.defaultValue);
	}

	/**
	 * Creates a new {@link DummyAttributeDefinition} with the specific default
	 * value.
	 *
	 * @param defaultValue the new default value
	 * @return a new {@link DummyAttributeDefinition} with the specific default
	 *         value
	 */
	public DummyAttributeDefinition withDefaultValue(String... defaultValue) {
		return new DummyAttributeDefinition(this.name, this.id, this.description, this.cardinality, this.type,
				this.optionValues, this.optionLabels, defaultValue);
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
	public int getCardinality() {
		return this.cardinality;
	}

	@Override
	public int getType() {
		return this.type;
	}

	@Override
	public String[] getOptionValues() {
		return this.optionValues;
	}

	@Override
	public String[] getOptionLabels() {
		return this.optionLabels;
	}

	@Override
	public String validate(String s) {
		return "";
	}

	@Override
	public String[] getDefaultValue() {
		return this.defaultValue;
	}

}
