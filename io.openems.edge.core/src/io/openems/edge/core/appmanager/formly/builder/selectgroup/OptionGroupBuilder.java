package io.openems.edge.core.appmanager.formly.builder.selectgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class OptionGroupBuilder {

	private final OptionGroup optionGroup;

	public OptionGroupBuilder(String group, String title) {
		super();
		this.optionGroup = new OptionGroup(group, title, new ArrayList<>());
	}

	/**
	 * Adds a {@link Option} to this {@link OptionGroup}.
	 * 
	 * @param option the {@link Option} to add
	 * @return this
	 */
	public OptionGroupBuilder addOption(Option option) {
		this.optionGroup.options().add(option);
		return this;
	}

	/**
	 * Adds a all {@link Option Option} to this {@link OptionGroup}.
	 * 
	 * @param <T>             the type of the list
	 * @param options         the options to add
	 * @param mappingFunction the mapping function from the list to an
	 *                        {@link Option}
	 * @return this
	 */
	public <T> OptionGroupBuilder addOptions(List<T> options, Function<T, Option> mappingFunction) {
		options.stream() //
				.map(mappingFunction) //
				.forEach(this.optionGroup.options()::add);
		return this;
	}

	public OptionGroup build() {
		return this.optionGroup;
	}

}