package io.openems.edge.relaisBoard;

import io.openems.edge.relaisboardmcp.Mcp;

public interface RelaisBoard {

    String getId();
    Mcp getMcp();
}
