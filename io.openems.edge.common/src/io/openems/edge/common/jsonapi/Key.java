package io.openems.edge.common.jsonapi;

public record Key<T>(String identifier, Class<T> type) {

}