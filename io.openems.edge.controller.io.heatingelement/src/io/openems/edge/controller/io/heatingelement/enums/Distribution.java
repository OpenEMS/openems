package io.openems.edge.controller.io.heatingelement.enums;

public enum Distribution {
	/**
	 * Heating Element A is fully activated before Heating Element B is activated:
	 * A-L1 -&gt; A-L2 -&gt; A-L3 -&gt; B-L1 -&gt; B-L2 -&gt; B-L3.
	 */
	SEQUENTIAL,
	/**
	 * Heating Elements A and B are activated alternately: A-L1 -&gt; B-L1 -&gt;
	 * A-L2 -&gt; B-L2 -&gt; A-L3 -&gt; B-L3.
	 */
	PARALLEL;
}
