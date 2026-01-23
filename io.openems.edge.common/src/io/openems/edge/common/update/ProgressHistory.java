package io.openems.edge.common.update;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressHistory {

	private final Logger log = LoggerFactory.getLogger(ProgressHistory.class);

	private final List<Consumer<ProgressHistory>> onChangeListeners = new ArrayList<>();
	private final List<Progress> steps = new ArrayList<>();

	/**
	 * Gets the entries as log messages.
	 * 
	 * @return a list with the log messages
	 */
	public List<String> asLog() {
		return this.steps.stream() //
				.map(Object::toString) //
				.toList();
	}

	/**
	 * Gets all progress steps.
	 * 
	 * @return the progress steps
	 */
	public List<Progress> getSteps() {
		return this.steps;
	}

	/**
	 * Gets the last progress item.
	 * 
	 * @return the last progress item; null if empty
	 */
	public Progress last() {
		if (this.steps.isEmpty()) {
			return null;
		}
		return this.steps.getLast();
	}

	/**
	 * Adds a Progress item.
	 * 
	 * @param progress the progress to add
	 */
	public void addProgress(Progress progress) {
		this.steps.add(progress);
		this.notifyOnChangeListeners();
	}

	/**
	 * Adds an on change listener. Triggered when a progress item gets added.
	 * 
	 * @param listener the listener to add
	 * @return the added listener
	 */
	public Consumer<ProgressHistory> addOnChangeListener(Consumer<ProgressHistory> listener) {
		this.onChangeListeners.add(listener);
		return listener;
	}

	/**
	 * Removes an on change listener.
	 * 
	 * @param listener the listener to remove
	 * @return true if the listener was in the list; else false
	 */
	public boolean removeOnChangeListener(Consumer<ProgressHistory> listener) {
		return this.onChangeListeners.remove(listener);
	}

	private void notifyOnChangeListeners() {
		for (var onChangeListener : this.onChangeListeners) {
			try {
				onChangeListener.accept(this);
			} catch (Exception e) {
				this.log.warn("Unexpected error while executing progress history listener", e);
			}
		}
	}

}
