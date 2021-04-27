package io.openems.edge.heater.gasboiler.viessmann;

public enum GasBoilerType {

    VITOTRONIC_100(8,9,5,6,
            47,7,10,11,
            12, 34,48,17, 18,
            49, 19,50,
            51, 52,53,
            31,32, 33,4,
            16, 11,30,
            16, 37,38,
            41, 39,40,
            27, 54,55,
            56, 57,42,
            5, 4,1,
            7, 8,13,
            8, 1,9,
            3, 10,4,
            11, 15,13,
            28, 29,17,
            1, 17,14
            );



    int     outPutAM1_1_Address, outPutAm1_2_Address, output20Address, output_29Address,
            ambientTemperatureAddress, outPutEA1Address, input_EA_1Address, input_EA_2Address, input_EA_3Address,
            setPoint_EA_1Address, outPutSignalPM1_Percent, gridVoltageBehaviourPM1Address, floatingElectricalContactPm1Address,
            volumeFlowSetPointPm1Address, disturbanceInputPM1Address, temperatureSensorPM_1_1Address,
            temperatureSensorPM1_2Address, temperatureSensorPM1_3Address, temperatureSensorPM1_4Address,
            rewindTemperature_17_A_Address,rewindTemperature_17_B_Address, sensor9address, tributaryPumpAddress, operatingModeA1_M1,
            boilerSetPointTemperatureAddress, combustionEngineExhaustTemperatureAddress, combustionEngineOnOffAddress,
            combustionEngineOperatingHoursTier1Address, combustionEngineOperatingHoursTier2Address,
            combustionEngineEfficiencyActualValueAddress, combustionEngineStartCounterAddress,
            combustionEngineOperatingModeAddress, combustionEngineBoilerTemperatureAddress,
            temperatureSensor_1_PM_1_StatusAddress, temperatureSensor_1_PM_2_StatusAddress,
            temperatureSensor_1_PM_3_StatusAddress, temperatureSensor_1_PM_4_StatusAddress,
            operatingHoursCombustionEngineTier_1_expandedAddress, heatBoilerOperationModeAddress,
            heatBoilerTemperatureSetPointEffectiveAddress, heatBoilerPerformanceStatusAddress,
            heatBoilerPerformanceSetPointStatusAddress, heatBoilerPerformanceSetPointValueAddress,
            heatBoilerTemperatureSetPointAddress, heatBoilerTemperatureActualAddress,
            heatBoilerModulationValueAddress, warmWaterOperationModeAddress,
            warmWaterEffectiveSetPointTemperatureAddress, functioningWarmWaterSetPointTemperatureAddress,
            boilerSetPointPerformanceEffectiveAddress, boilerSetPointTemperatureEffectiveAddress,
            boilerMaxReachedExhaustTemperatureAddress, warmWaterStorageChargePumpAddress,
            warmWaterStorageTemperature_5_A_Address, warmWaterStorageTemperature_5_B_Address,
            warmWaterPreparationAddress, warmWaterTemperatureSetPointAddress,
            warmWaterSetPointEffectiveAddress, warmWaterCirculationPumpAddress;

    GasBoilerType(
            int outPutAM1_1_Address, int outPutAm1_2_Address, int output20Address, int output_29Address,
            int ambientTemperatureAddress, int outPutEA1Address, int input_EA_1Address, int input_EA_2Address, int input_EA_3Address,
            int setPoint_EA_1Address, int outPutSignalPM1_Percent, int gridVoltageBehaviourPM1Address,int floatingElectricalContactPm1Address,
            int volumeFlowSetPointPm1Address, int disturbanceInputPM1Address, int temperatureSensorPM_1_1Address,
            int temperatureSensorPM1_2Address, int temperatureSensorPM1_3Address, int temperatureSensorPM1_4Address,
            int rewindTemperature_17_A_Address, int rewindTemperature_17_B_Address, int sensor9address, int tributaryPumpAddress, int operatingModeA1_M1,
            int boilerSetPointTemperatureAddress, int combustionEngineExhaustTemperatureAddress, int combustionEngineOnOffAddress,
            int combustionEngineOperatingHoursTier1Address, int combustionEngineOperatingHoursTier2Address,
            int combustionEngineEfficiencyActualValueAddress, int combustionEngineStartCounterAddress,
            int combustionEngineOperatingModeAddress, int combustionEngineBoilerTemperatureAddress,
            int temperatureSensor_1_PM_1_StatusAddress, int temperatureSensor_1_PM_2_StatusAddress,
            int temperatureSensor_1_PM_3_StatusAddress, int temperatureSensor_1_PM_4_StatusAddress,
            int operatingHoursCombustionEngineTier_1_expandedAddress, int heatBoilerOperationModeAddress,
            int heatBoilerTemperatureSetPointEffectiveAddress, int heatBoilerPerformanceStatusAddress,
            int heatBoilerPerformanceSetPointStatusAddress, int heatBoilerPerformanceSetPointValueAddress,
            int heatBoilerTemperatureSetPointAddress, int heatBoilerTemperatureActualAddress,
            int heatBoilerModulationValueAddress, int warmWaterOperationModeAddress,
            int warmWaterEffectiveSetPointTemperatureAddress, int functioningWarmWaterSetPointTemperatureAddress,
            int boilerSetPointPerformanceEffectiveAddress, int boilerSetPointTemperatureEffectiveAddress,
            int boilerMaxReachedExhaustTemperatureAddress, int warmWaterStorageChargePumpAddress,
            int warmWaterStorageTemperature_5_A_Address, int warmWaterStorageTemperature_5_B_Address,
            int warmWaterPreparationAddress, int warmWaterTemperatureSetPointAddress,
            int warmWaterSetPointEffectiveAddress, int warmWaterCirculationPumpAddress
    ) {

        this.outPutAM1_1_Address = outPutAM1_1_Address;
        this.outPutAm1_2_Address = outPutAm1_2_Address;
        this.output20Address = output20Address;
        this.output_29Address = output_29Address;
        this.ambientTemperatureAddress = ambientTemperatureAddress;
        this.outPutEA1Address = outPutEA1Address;
        this.input_EA_1Address = input_EA_1Address;
        this.input_EA_2Address = input_EA_2Address;
        this.input_EA_3Address = input_EA_3Address;

        this.setPoint_EA_1Address = setPoint_EA_1Address;
        this.outPutSignalPM1_Percent = outPutSignalPM1_Percent;
        this.gridVoltageBehaviourPM1Address = gridVoltageBehaviourPM1Address;
        this.floatingElectricalContactPm1Address = floatingElectricalContactPm1Address;

        this.volumeFlowSetPointPm1Address = volumeFlowSetPointPm1Address;
        this.disturbanceInputPM1Address = disturbanceInputPM1Address;
        this.temperatureSensorPM_1_1Address = temperatureSensorPM_1_1Address;

        this.temperatureSensorPM1_2Address = temperatureSensorPM1_2Address;
        this.temperatureSensorPM1_3Address = temperatureSensorPM1_3Address;
        this.temperatureSensorPM1_4Address = temperatureSensorPM1_4Address;

        this.rewindTemperature_17_A_Address = rewindTemperature_17_A_Address;
        this.rewindTemperature_17_B_Address = rewindTemperature_17_B_Address;
        this.sensor9address = sensor9address;
        this.tributaryPumpAddress = tributaryPumpAddress;
        this.operatingModeA1_M1 = operatingModeA1_M1;

        this.boilerSetPointTemperatureAddress = boilerSetPointTemperatureAddress;
        this.combustionEngineExhaustTemperatureAddress = combustionEngineExhaustTemperatureAddress;
        this.combustionEngineOnOffAddress = combustionEngineOnOffAddress;

        this.combustionEngineOperatingHoursTier1Address = combustionEngineOperatingHoursTier1Address;
        this.combustionEngineOperatingHoursTier2Address = combustionEngineOperatingHoursTier2Address;

        this.combustionEngineEfficiencyActualValueAddress = combustionEngineEfficiencyActualValueAddress;
        this.combustionEngineStartCounterAddress = combustionEngineStartCounterAddress;

        this.combustionEngineOperatingModeAddress = combustionEngineOperatingModeAddress;
        this.combustionEngineBoilerTemperatureAddress = combustionEngineBoilerTemperatureAddress;

        this.temperatureSensor_1_PM_1_StatusAddress = temperatureSensor_1_PM_1_StatusAddress;
        this.temperatureSensor_1_PM_2_StatusAddress = temperatureSensor_1_PM_2_StatusAddress;

        this.temperatureSensor_1_PM_3_StatusAddress = temperatureSensor_1_PM_3_StatusAddress;
        this.temperatureSensor_1_PM_4_StatusAddress = temperatureSensor_1_PM_4_StatusAddress;

        this.operatingHoursCombustionEngineTier_1_expandedAddress = operatingHoursCombustionEngineTier_1_expandedAddress;
        this.heatBoilerOperationModeAddress = heatBoilerOperationModeAddress;

        this.heatBoilerTemperatureSetPointEffectiveAddress = heatBoilerTemperatureSetPointEffectiveAddress;
        this.heatBoilerPerformanceStatusAddress = heatBoilerPerformanceStatusAddress;

        this.heatBoilerPerformanceSetPointStatusAddress = heatBoilerPerformanceSetPointStatusAddress;
        this.heatBoilerPerformanceSetPointValueAddress = heatBoilerPerformanceSetPointValueAddress;

        this.heatBoilerTemperatureSetPointAddress = heatBoilerTemperatureSetPointAddress;
        this.heatBoilerTemperatureActualAddress = heatBoilerTemperatureActualAddress;

        this.heatBoilerModulationValueAddress = heatBoilerModulationValueAddress;
        this.warmWaterOperationModeAddress = warmWaterOperationModeAddress;

        this.warmWaterEffectiveSetPointTemperatureAddress = warmWaterEffectiveSetPointTemperatureAddress;
        this.functioningWarmWaterSetPointTemperatureAddress = functioningWarmWaterSetPointTemperatureAddress;

        this.boilerSetPointPerformanceEffectiveAddress = boilerSetPointPerformanceEffectiveAddress;
        this.boilerSetPointTemperatureEffectiveAddress = boilerSetPointTemperatureEffectiveAddress;

        this.boilerMaxReachedExhaustTemperatureAddress = boilerMaxReachedExhaustTemperatureAddress;
        this.warmWaterStorageChargePumpAddress = warmWaterStorageChargePumpAddress;

        this.warmWaterStorageTemperature_5_A_Address = warmWaterStorageTemperature_5_A_Address;
        this.warmWaterStorageTemperature_5_B_Address = warmWaterStorageTemperature_5_B_Address;

        this.warmWaterPreparationAddress = warmWaterPreparationAddress;
        this.warmWaterTemperatureSetPointAddress = warmWaterTemperatureSetPointAddress;

        this.warmWaterSetPointEffectiveAddress = warmWaterSetPointEffectiveAddress;
        this.warmWaterCirculationPumpAddress = warmWaterCirculationPumpAddress;

    }


    public int getOutPutAM1_1_Address() {
        return outPutAM1_1_Address;
    }

    public int getOutPutAm1_2_Address() {
        return outPutAm1_2_Address;
    }

    public int getOutput20Address() {
        return output20Address;
    }

    public int getOutput_29Address() {
        return output_29Address;
    }

    public int getAmbientTemperatureAddress() {
        return ambientTemperatureAddress;
    }

    public int getOutPutEA1Address() {
        return outPutEA1Address;
    }

    public int getInput_EA_1Address() {
        return input_EA_1Address;
    }

    public int getInput_EA_2Address() {
        return input_EA_2Address;
    }

    public int getInput_EA_3Address() {
        return input_EA_3Address;
    }

    public int getSetPoint_EA_1Address() {
        return setPoint_EA_1Address;
    }

    public int getOutPutSignalPM1_Percent() {
        return outPutSignalPM1_Percent;
    }

    public int getGridVoltageBehaviourPM1Address() {
        return gridVoltageBehaviourPM1Address;
    }

    public int getFloatingElectricalContactPm1Address() {
        return floatingElectricalContactPm1Address;
    }

    public int getVolumeFlowSetPointPm1Address() {
        return volumeFlowSetPointPm1Address;
    }

    public int getDisturbanceInputPM1Address() {
        return disturbanceInputPM1Address;
    }

    public int getTemperatureSensorPM_1_1Address() {
        return temperatureSensorPM_1_1Address;
    }

    public int getTemperatureSensorPM1_2Address() {
        return temperatureSensorPM1_2Address;
    }

    public int getTemperatureSensorPM1_3Address() {
        return temperatureSensorPM1_3Address;
    }

    public int getTemperatureSensorPM1_4Address() {
        return temperatureSensorPM1_4Address;
    }

    public int getRewindTemperature_17_A_Address() {
        return rewindTemperature_17_A_Address;
    }

    public int getRewindTemperature_17_B_Address() {
        return rewindTemperature_17_B_Address;
    }

    public int getSensor9address() {
        return sensor9address;
    }

    public int getTributaryPumpAddress() {
        return tributaryPumpAddress;
    }

    public int getOperatingModeA1_M1() {
        return operatingModeA1_M1;
    }

    public int getBoilerSetPointTemperatureAddress() {
        return boilerSetPointTemperatureAddress;
    }

    public int getCombustionEngineExhaustTemperatureAddress() {
        return combustionEngineExhaustTemperatureAddress;
    }

    public int getCombustionEngineOnOffAddress() {
        return combustionEngineOnOffAddress;
    }

    public int getCombustionEngineOperatingHoursTier1Address() {
        return combustionEngineOperatingHoursTier1Address;
    }

    public int getCombustionEngineOperatingHoursTier2Address() {
        return combustionEngineOperatingHoursTier2Address;
    }

    public int getCombustionEngineEfficiencyActualValueAddress() {
        return combustionEngineEfficiencyActualValueAddress;
    }

    public int getCombustionEngineStartCounterAddress() {
        return combustionEngineStartCounterAddress;
    }

    public int getCombustionEngineOperatingModeAddress() {
        return combustionEngineOperatingModeAddress;
    }

    public int getCombustionEngineBoilerTemperatureAddress() {
        return combustionEngineBoilerTemperatureAddress;
    }

    public int getTemperatureSensor_1_PM_1_StatusAddress() {
        return temperatureSensor_1_PM_1_StatusAddress;
    }

    public int getTemperatureSensor_1_PM_2_StatusAddress() {
        return temperatureSensor_1_PM_2_StatusAddress;
    }

    public int getTemperatureSensor_1_PM_3_StatusAddress() {
        return temperatureSensor_1_PM_3_StatusAddress;
    }

    public int getTemperatureSensor_1_PM_4_StatusAddress() {
        return temperatureSensor_1_PM_4_StatusAddress;
    }

    public int getOperatingHoursCombustionEngineTier_1_expandedAddress() {
        return operatingHoursCombustionEngineTier_1_expandedAddress;
    }

    public int getHeatBoilerOperationModeAddress() {
        return heatBoilerOperationModeAddress;
    }

    public int getHeatBoilerTemperatureSetPointEffectiveAddress() {
        return heatBoilerTemperatureSetPointEffectiveAddress;
    }

    public int getHeatBoilerPerformanceStatusAddress() {
        return heatBoilerPerformanceStatusAddress;
    }

    public int getHeatBoilerPerformanceSetPointStatusAddress() {
        return heatBoilerPerformanceSetPointStatusAddress;
    }

    public int getHeatBoilerPerformanceSetPointValueAddress() {
        return heatBoilerPerformanceSetPointValueAddress;
    }

    public int getHeatBoilerTemperatureSetPointAddress() {
        return heatBoilerTemperatureSetPointAddress;
    }

    public int getHeatBoilerTemperatureActualAddress() {
        return heatBoilerTemperatureActualAddress;
    }

    public int getHeatBoilerModulationValueAddress() {
        return heatBoilerModulationValueAddress;
    }

    public int getWarmWaterOperationModeAddress() {
        return warmWaterOperationModeAddress;
    }

    public int getWarmWaterEffectiveSetPointTemperatureAddress() {
        return warmWaterEffectiveSetPointTemperatureAddress;
    }

    public int getFunctioningWarmWaterSetPointTemperatureAddress() {
        return functioningWarmWaterSetPointTemperatureAddress;
    }

    public int getBoilerSetPointPerformanceEffectiveAddress() {
        return boilerSetPointPerformanceEffectiveAddress;
    }

    public int getBoilerSetPointTemperatureEffectiveAddress() {
        return boilerSetPointTemperatureEffectiveAddress;
    }

    public int getBoilerMaxReachedExhaustTemperatureAddress() {
        return boilerMaxReachedExhaustTemperatureAddress;
    }

    public int getWarmWaterStorageChargePumpAddress() {
        return warmWaterStorageChargePumpAddress;
    }

    public int getWarmWaterStorageTemperature_5_A_Address() {
        return warmWaterStorageTemperature_5_A_Address;
    }

    public int getWarmWaterStorageTemperature_5_B_Address() {
        return warmWaterStorageTemperature_5_B_Address;
    }

    public int getWarmWaterPreparationAddress() {
        return warmWaterPreparationAddress;
    }

    public int getWarmWaterTemperatureSetPointAddress() {
        return warmWaterTemperatureSetPointAddress;
    }

    public int getWarmWaterSetPointEffectiveAddress() {
        return warmWaterSetPointEffectiveAddress;
    }

    public int getWarmWaterCirculationPumpAddress() {
        return warmWaterCirculationPumpAddress;
    }
}
