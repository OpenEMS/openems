package io.openems.edge.meter.heatmeter;


public enum HeatMeterModel {
    //HeatMeter Types with their address for Mbus
    ITRON_CF_51(3, 4, 1, 5, 6),
    SHARKY_775(4, 5, 0, 6, 7),
    ZELSIUS_C5_CMF(0,0,0,0,0),
    ELSTER_SENSOSTAR_2(3,5,1,7,8);


    int powerAddress;
    int flowRateAddress;
    int totalConsumptionEnergyAddress;
    int flowTempAddress;
    int returnTempAddress;

    HeatMeterModel(int power, int flowRate, int totalConsumptionEnergy, int flowTemp, int returnTemp) {
        this.powerAddress = power;
        this.flowRateAddress = flowRate;
        this.totalConsumptionEnergyAddress = totalConsumptionEnergy;

        this.flowTempAddress = flowTemp;
        this.returnTempAddress = returnTemp;
    }


    public int getPowerAddress() {
        return powerAddress;
    }

    public int getFlowRateAddress() {
        return flowRateAddress;
    }

    public int getTotalConsumptionEnergyAddress() {
        return totalConsumptionEnergyAddress;
    }

    public int getFlowTempAddress() {
        return flowTempAddress;
    }

    public int getReturnTempAddress() {
        return returnTempAddress;
    }
}
