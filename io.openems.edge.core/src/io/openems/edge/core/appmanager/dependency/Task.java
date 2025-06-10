package io.openems.edge.core.appmanager.dependency;

import io.openems.edge.core.appmanager.dependency.aggregatetask.AggregateTask;

public record Task<T>(Class<? extends AggregateTask<T>> aggregateTaskClass, T configuration) {

}