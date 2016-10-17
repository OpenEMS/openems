package io.openems.api.controller;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.openems.api.thing.Thing;

@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface IsThingMap {
	Class<? extends Thing> type();
}
