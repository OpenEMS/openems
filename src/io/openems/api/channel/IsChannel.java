package io.openems.api.channel;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface IsChannel {
	String id();
	// TODO add unit and check if Channel-Implementation has the same
}
