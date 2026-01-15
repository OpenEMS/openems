package io.openems.edge.common.jsonapi;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.jscalendar.AddTask;
import io.openems.common.jscalendar.DeleteTask;
import io.openems.common.jscalendar.GetAllTasks;
import io.openems.common.jscalendar.GetOneTasks;
import io.openems.common.jscalendar.GetTask;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.jscalendar.UpdateTask;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;

// CHECKSTYLE:OFF
public class JSCalendarApi {
	// CHECKSTYLE:ON

	private static final Logger LOG = LoggerFactory.getLogger(JSCalendarApi.class);

	private JSCalendarApi() {
	}

	/**
	 * Adds Request Handlers to {@link JsonApiBuilder} for {@link JSCalendar}
	 * operations.
	 * 
	 * @param <PAYLOAD>         the type of the Payload
	 * @param builder           the {@link JsonApiBuilder}
	 * @param payloadSerializer a {@link JsonSerializer} for the Payload
	 * @param tasksSupplier     a {@link Supplier} for {@link JSCalendar.Tasks}
	 * @param ur                a {@link Supplier} for
	 *                          {@link UpdateJsCalendarRecord}
	 */
	public static <PAYLOAD> void buildJsonApiRoutes(JsonApiBuilder builder, JsonSerializer<PAYLOAD> payloadSerializer,
			Supplier<JSCalendar.Tasks<PAYLOAD>> tasksSupplier, Supplier<UpdateJsCalendarRecord> ur) {
		builder.handleRequest(new AddTask<PAYLOAD>(payloadSerializer), call -> {
			var newTask = call.getRequest().task();
			var updatedTasks = tasksSupplier.get().withAddedTask(newTask);
			updateJsCalendarProperty(payloadSerializer, ur.get(), call.get(EdgeKeys.USER_KEY), updatedTasks);
			return new AddTask.Response(newTask.uid());
		});

		builder.handleRequest(new UpdateTask<PAYLOAD>(payloadSerializer), call -> {
			var updatedTask = call.getRequest().task();
			var updatedTasks = tasksSupplier.get().withUpdatedTask(updatedTask);
			updateJsCalendarProperty(payloadSerializer, ur.get(), call.get(EdgeKeys.USER_KEY), updatedTasks);
			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(new DeleteTask(), call -> {
			var uidToRemove = call.getRequest().uid();
			var updatedTasks = tasksSupplier.get().withRemovedTask(uidToRemove);
			updateJsCalendarProperty(payloadSerializer, ur.get(), call.get(EdgeKeys.USER_KEY), updatedTasks);
			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(new GetTask<PAYLOAD>(payloadSerializer), call -> {
			var uid = call.getRequest().uid();
			var task = tasksSupplier.get().tasks.stream()//
					.filter(t -> t.uid().equals(uid))//
					.collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
						if (list.size() != 1) {
							throw new IllegalStateException("Expected exactly one task, but found " + list.size());
						}
						return list.get(0);
					}));

			return new GetTask.Response<PAYLOAD>(task);
		});

		builder.handleRequest(new GetAllTasks<PAYLOAD>(payloadSerializer), call -> {
			return new GetAllTasks.Response<PAYLOAD>(tasksSupplier.get().tasks);
		});

		builder.handleRequest(new GetOneTasks<PAYLOAD>(payloadSerializer), call -> {
			var from = call.getRequest().from();
			var to = call.getRequest().to();
			var oneTasks = tasksSupplier.get().getOneTasksBetween(from, to);
			return new GetOneTasks.Response<PAYLOAD>(oneTasks);
		});
	}

	public static record UpdateJsCalendarRecord(ConfigurationAdmin cm, ComponentManager componentManager,
			String servicePid, String propertyKey) {
	}

	private static synchronized <PAYLOAD> void updateJsCalendarProperty(JsonSerializer<PAYLOAD> payloadSerializer,
			UpdateJsCalendarRecord ur, User user, Tasks<PAYLOAD> tasks) {
		try {
			var config = ur.cm.getConfiguration(ur.servicePid, "?");
			var properties = config.getProperties();

			properties.put(ur.propertyKey, tasks.toJson(payloadSerializer).toString());

			var lastChangeBy = (user != null) //
					? user.getId() + ": " + user.getName() //
					: "UNDEFINED";
			properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, lastChangeBy);
			properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
					LocalDateTime.now(ur.componentManager.getClock()).truncatedTo(ChronoUnit.SECONDS).toString());

			config.update(properties);

		} catch (IOException e) {
			LOG.error(ur.servicePid + ": Unable to update JSCalendar Property. " + e.getMessage());
		}
	}
}
