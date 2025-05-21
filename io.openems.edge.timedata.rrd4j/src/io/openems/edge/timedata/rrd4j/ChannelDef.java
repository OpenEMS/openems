package io.openems.edge.timedata.rrd4j;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;

public record ChannelDef(//
		DsType dsType, //
		double minValue, //
		double maxValue, //
		ConsolFun consolFun //
) {

}
