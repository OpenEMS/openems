package io.openems.impl.scheduler;

import java.util.Collections;

import io.openems.api.controller.Controller;
import io.openems.core.databus.Databus;
import io.openems.core.scheduler.Scheduler;

public class SimpleScheduler extends Scheduler {
	public SimpleScheduler(Databus databus) {
		super(databus);
	}

	@Override
	public void activate() {
		log.debug("Activate SimpleScheduler");
		super.activate();
	}

	@Override
	protected void dispose() {
	}

	@Override
	protected void forever() {
		Collections.sort(controllers, (c1, c2) -> c1.getPriority() - c2.getPriority());
		for (Controller controller : controllers) {
			log.info("Controller: " + controller);
			controller.run();
		}
		// lastExecution = System.currentTimeMillis();
		// writtenChannels.clear();
		// rangeCache.clear();
		// for (Controller c : controller) {
		// if (isExecutionAllowed(c)) {
		// updateReadMappings(c);
		// // TODO Timeout
		// c.run();
		//
		// checkWriteMappings(c);
		// }
		// }
		// try {
		// if (wait - (System.currentTimeMillis() - lastExecution) > 0) {
		// Thread.sleep(wait - (System.currentTimeMillis() - lastExecution));
		// }
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	@Override
	protected boolean initialize() {
		return true;
	}

	// private void checkWriteMappings(Controller c)
	// throws IllegalArgumentException, IllegalAccessException, ChannelWriteException, RangeModificationException {
	// HashMap<Channel, Range> writtenRanges = new HashMap<>();
	// HashMap<Channel, Range> ranges = new HashMap<>();
	// for (ChannelMapping cm : writeFieldMapping.get(c)) {
	// Range r = cm.getWriteRange();
	// if (r != null) {
	// ranges.put(cm.getChannel(), r);
	// if (r.getWriteValue() != null) {
	// writtenRanges.put(cm.getChannel(), r);
	// }
	// }
	// }
	// for (Entry<Channel, Range> r : ranges.entrySet()) {
	// rangeCache.put(r.getKey(), r.getValue());
	// }
	// for (Entry<Channel, Range> wr : writtenRanges.entrySet()) {
	// System.out.println("Controller " + c.getName() + " has written Value "
	// + wr.getValue().getWriteValue().asString() + " on Field " + wr.getKey().getId());
	// wr.getKey().write(wr.getValue().getWriteValue());
	// writtenChannels.add(wr.getKey());
	// }
	// }
	//
	// private boolean isExecutionAllowed(Controller c) {
	// List<ChannelMapping> mappings = writeFieldMapping.get(c);
	// for (ChannelMapping mapping : mappings) {
	// if (writtenChannels.contains(mapping.getChannel())) {
	// return false;
	// }
	// }
	// return true;
	// }
	//
	// private void updateReadMappings(Controller c) throws IllegalArgumentException, IllegalAccessException {
	// for (ChannelMapping cm : readFieldMapping.get(c)) {
	// Range r = rangeCache.get(cm.getChannel());
	// if (r != null) {
	// cm.updateField(r);
	// } else {
	// cm.updateField();
	// }
	// }
	// }
}
