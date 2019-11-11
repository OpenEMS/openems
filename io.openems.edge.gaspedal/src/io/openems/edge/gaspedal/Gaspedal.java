package io.openems.edge.gaspedal;

import io.openems.edge.relaisboardmcp.Mcp;

public interface Gaspedal {

    String getId();
    Mcp getMcp();
    String calculateValues(String percentageRange, int position, String powerLevel);
}
