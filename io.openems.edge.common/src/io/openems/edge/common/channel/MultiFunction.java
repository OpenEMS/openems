package io.openems.edge.common.channel;

import java.util.Collection;

public interface MultiFunction<T extends Channel<?>, R> {

	T apply(Collection<R> sourceValues);

}
