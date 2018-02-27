package io.openems.core.utilities.websocket;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.openems.api.controller.Controller;
import io.openems.api.scheduler.Scheduler;
import io.openems.core.ThingRepository;
import io.openems.impl.controller.api.websocket.WebsocketApiController;
import io.openems.impl.persistence.fenecon.FeneconPersistence;

public class WebsocketLogAppender extends AppenderBase<ILoggingEvent> {

	@Override
	protected void append(ILoggingEvent event) {
		long timestamp = event.getTimeStamp();
		String level = event.getLevel().toString();
		String source = event.getLoggerName();
		String message = event.getFormattedMessage();

		ThingRepository thingRepository = ThingRepository.getInstance();
		for (Scheduler scheduler : thingRepository.getSchedulers()) {
			for (Controller controller : scheduler.getControllers()) {
				if (controller instanceof WebsocketApiController) {
					WebsocketApiController websocketApiController = (WebsocketApiController) controller;
					websocketApiController.broadcastLog(timestamp, level, source, message);
				}
			}
		}

		// send to fenecon persistence
		ThingRepository.getInstance().getPersistences().forEach((persistence) -> {
			if (persistence instanceof FeneconPersistence) {
				FeneconPersistence p = (FeneconPersistence) persistence;
				p.getWebsocketHandler().sendLog(timestamp, level, source, message);
			}
		});
	}

}
