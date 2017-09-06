package io.openems.api.channel;

import java.util.ArrayList;
import java.util.List;

import io.openems.api.thing.Thing;
import io.openems.core.Databus;

public class DebugChannel<T> extends ReadChannel<T> {
	private static List<DebugChannel<?>> debugChannels = new ArrayList<>();
	private static boolean DEBUGENABLED = false;
	private static Databus bus = Databus.getInstance();

	public static void enableDebug() {
		DEBUGENABLED = true;
		for (DebugChannel<?> channel : debugChannels) {
			channel.addChangeListener(bus);
			channel.addUpdateListener(bus);
		}
	}

	public static void disableDebug() {
		DEBUGENABLED = false;
		for (DebugChannel<?> channel : debugChannels) {
			channel.removeChangeListener(bus);
			channel.removeUpdateListener(bus);
		}
	}

	public DebugChannel(String id, Thing parent) {
		super(id, parent);
		if (DEBUGENABLED) {
			this.addChangeListener(bus);
			this.addUpdateListener(bus);
		}
		debugChannels.add(this);
	}

	public void setValue(T value) {
		this.updateValue(value);
	}

}
