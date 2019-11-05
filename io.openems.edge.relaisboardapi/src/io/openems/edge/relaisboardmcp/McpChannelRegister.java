package io.openems.edge.relaisboardmcp;

public interface McpChannelRegister {
    void setPosition(int position, boolean activate);

    void shift();
    void addToDefault(int position, boolean activate);
}
