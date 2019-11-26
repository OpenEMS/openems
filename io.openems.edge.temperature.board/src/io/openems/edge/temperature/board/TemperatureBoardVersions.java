package io.openems.edge.temperature.board;

import io.openems.edge.temperature.board.api.Adc;
import io.openems.edge.temperature.board.api.mcpModels.type8.Mcp3208;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum TemperatureBoardVersions {

    TEMPERATURE_BOARD_V_1(0.0000038937,0.021592132466482,-40.8774465191316, (short)16, new Mcp3208(), new Mcp3208());


    private final double regressionValueA;
    private final double regressionValueB;
    private final double regressionValueC;
    private List<Adc> mcpContainer = new ArrayList<>();
    private final short maxSize;

    TemperatureBoardVersions(double regressionValueA, double regressionValueB, double regression_value_c, short maxSize, Adc... mcpCollection) {


        this.regressionValueA = regressionValueA;
        this.regressionValueB = regressionValueB;
        this.regressionValueC = regression_value_c;
        mcpContainer.addAll(Arrays.asList(mcpCollection));
        this.maxSize = maxSize;
    }


    public double getRegressionValueA() {
        return regressionValueA;
    }

    public double getRegressionValueB() {
        return regressionValueB;
    }

    public double getRegressionValueC() {
        return regressionValueC;
    }

    public List<Adc> getMcpContainer() {
        return mcpContainer;
    }

    public short getMaxSize() {
        return maxSize;
    }
}