package io.openems.edge.relais.board;

import io.openems.edge.relais.board.api.Mcp;

public interface RelaisBoard {

    String getId();

    Mcp getMcp();
}
