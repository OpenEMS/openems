package io.openems.edge.relaisBoard.api;

public interface McpChannelRegister {

    void setPosition(int position, boolean activate);

    void shift();
    void addToDefault(int position, boolean activate);
}
