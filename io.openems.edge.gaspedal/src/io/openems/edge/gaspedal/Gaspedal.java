package io.openems.edge.gaspedal;

import io.openems.edge.relais.board.api.Mcp;

public interface Gaspedal {

    String getId();

    Mcp getMcp();


}
