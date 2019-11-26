package io.openems.edge.relaisBoard;

import io.openems.edge.relaisboardapi.Mcp;

public interface RelaisBoard {

    String getId();
    Mcp getMcp();
}
