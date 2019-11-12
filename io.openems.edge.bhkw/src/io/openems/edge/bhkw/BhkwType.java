package io.openems.edge.bhkw;

public enum BhkwType {
//    BHKW60390(),
//    BHKW260390(),
//    BHKW199263(,,,,,90,70),
//    BHKW530660(),
//    BHKW530660S(),
//    BHKW140207(,,,,,94,75),
//    BHKW100167(),
//    BHKW401549(,,,,,90,70),
//    BHKW430580(),
//    BHKW920(),
//    BHKW615(),
//    BHKW5081(50,,,,0,93,75),
//    BHKW2039(),
//    BHKW70115(,,,,,92,75),
//    BHKW238363(,,,,,90,75),

    Vito_EM_50_81(50,4,20, 50,81,145, 93, 75),
    Vito_EM_70_115(50,4,20,70,115,204, 92, 75),
    Vito_EM_140_207(50,4,20,140,207,384, 94, 75),
    Vito_EM_199_263(50,4,20,199,263,538, 90, 70),
    Vito_EM_199_293(50,4,20,199,293,553, , ),
    Vito_EM_238_363(50,4,20,238,363,667, 90, 75),
    Vito_EM_363_498(50,4,20,363,498,960, , ),
    Vito_EM_401_546(50,4,20,401,549,1053, 90, 70),
    Vito_EM_530_660(50,4,20,530,660,1342),
    Vito_BM_36_66(50,4,20,36,66,122),
    Vito_BM_55_88(50,4,20,55,88,165),
    Vito_BM_190_238(50,4,20,190,238,493),
    Vito_BM_366_437(50,4,20,366,437,950)

;

    private int percentageRange;
    private int minValueAmpere;
    private int maxValueAmpere;
    //values in kW
    private int electricalOutput;
    private int thermalOutput;
    private float fuelUse;
    //values in °C
    private int maxFlowTemperature;
    private int maxReturnTemperature;

    BhkwType(int percentageRange, int minValueAmpere, int maxValueAmpere, int electricalOutput, int thermalOutput, float fuelUse, int maxFlowTemperature, int maxReturnTemperature) {
        this.percentageRange = percentageRange;
        this.minValueAmpere = minValueAmpere;
        this.maxValueAmpere = maxValueAmpere;
        this.electricalOutput = electricalOutput;
        this.thermalOutput = thermalOutput;
        this.fuelUse = fuelUse;
        this.maxFlowTemperature = maxFlowTemperature;
        this.maxReturnTemperature = maxReturnTemperature;


    }

    public int getPercentageRange() {
        return percentageRange;
    }

    public int getMinValueAmpere() {
        return minValueAmpere;
    }

    public int getMaxValueAmpere() {
        return maxValueAmpere;
    }

    public int getElectricalOutput() {
        return electricalOutput;
    }

    public int getThermalOutput() {
        return thermalOutput;
    }

    public float getFuelUse() {
        return fuelUse;
    }
}
