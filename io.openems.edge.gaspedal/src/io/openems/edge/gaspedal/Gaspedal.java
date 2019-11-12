package io.openems.edge.gaspedal;

import io.openems.edge.relaisboardmcp.Mcp;

public interface Gaspedal {

    String getId();

    Mcp getMcp();

    int calculateValueForDigit(int percentageRange, int ampereRange);

}
