package io.openems.common.jsonrpc.serialization;

import java.util.UUID;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsRuntimeException;

public class StringPathActual implements StringPath {

	private final String element;

	public StringPathActual(JsonElement element) throws OpenemsRuntimeException {
		super();
		if (!element.isJsonPrimitive() //
				|| !element.getAsJsonPrimitive().isString()) {
			throw new OpenemsRuntimeException(element + " is not a String!");
		}
		this.element = element.getAsString();
	}

	@Override
	public String get() {
		return this.element;
	}

	@Override
	public UUID getAsUuid() {
		return UUID.fromString(this.element);
	}

}
