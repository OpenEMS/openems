package io.openems.edge.bhkw;

public enum BhkwType {
    Vito_EM_6_15(0, 0, 20, 6, 14.9f, 22.2f, -1, -1, 7, 14.9f, 4.5f, 3.5f, "Vitobloc_200_EM_6_15"),
    Vito_EM_9_20(0, 0, 20, 8.5f, 20.1f, 30.1f, -1, -1, 7, 20.1f, 16.1f, 12.3f,"Vitobloc_200_EM_9_20"),
    Vito_EM_20_39(0, 0, 20, 20, 39, 62, -1, -1, 7, 39, 27.5f, 22.3f,"Vitobloc_200_EM_20_39"),
    Vito_EM_20_39_RL_70(0, 0, 20, 20, 39, 62, -1, -1, 7, 35.7f, 30.6f, 23.1f, "Vitobloc_200_EM_20_39_70"),
    Vito_EM_50_81(0, 0, 20, 50, 83, 145, 93, 75, 7, 83, 64, 46, "Vitobloc_200_EM_50_81"),
    Vito_EM_70_115(0, 0, 20, 70, 117, 204, 92, 75, 7, 117, 85, 66, "Vitobloc_200_EM_70_115"),
    Vito_Em_100_167(0,4,20,99,173,280,-1,-1,7,167,135,105, "Vitobloc_200_EM_100_167"),
    Vito_EM_140_207(0, 0, 20, 140, 207, 384, 94, 75, 7, 209, 171, 130, "Vitobloc_200_EM_140_207"),
    Vito_EM_199_263(0, 0, 20, 190, 278, 516, 90, 70, 7, 278, 235, 180, "Vitobloc_200_EM_199_263"),
    Vito_EM_199_293(0, 0, 20, 199, 293, 553, -1, -1, 7, 278, 235, 180, "Vitobloc_200_EM_199_293"),
    Vito_EM_238_363(0, 0, 20, 238, 363, 667, 90, 75, -1, -1, -1, -1, "Vitobloc_200_EM_238_363"),
    Vito_EM_363_498(0, 0, 20, 363, 498, 960, -1, -1, 7, 499, 404, 302, "Vitobloc_200_EM_363_498"),
    Vito_EM_401_549(0, 0, 20, 401, 552, 1053, 90, 70, 7, 552, 423, 316, "Vitobloc_200_EM_401_549"),
    Vito_EM_530_660(0, 0, 20, 530, 660, 1342, -1, -1, 7, 660, 590, 463, "Vitobloc_200_EM_530_660"),
    Vito_BM_36_66(0, 0, 20, 36, 66, 122, -1, -1, -1, -1, -1, -1, "Vitobloc_200_BM_36_66"),
    Vito_BM_55_88(0, 0, 20, 55, 88, 165,-1,-1,-1,-1,-1,-1, "Vitobloc_200_BM_55_88"),
    Vito_BM_190_238(0, 0, 20, 190, 238, 493,-1,-1,-1,-1,-1,-1, "Vitobloc_200_BM_190_238"),
    Vito_BM_366_437(0, 0, 20, 366, 437, 950,-1,-1,-1,-1,-1,-1, "Vitobloc_200_BM_366_437");


    private int percentageRange;
    private int minValueAmpere;
    private int maxValueAmpere;
    //tolerance in Percent
    private int warmingPerformanceTolerance;
    //values in kW
    private float electricalOutput;
    private float thermalOutput;
    private float fuelUse;
    //in kW and only high temperature natural gas
    private float warmingPerformance100Percent;
    private float warmingPerformance75Percent;
    private float warmingPerformance50Percent;

    //values in °C
    private int maxFlowTemperature;
    private int maxReturnTemperature;

    private String name;

    BhkwType(int percentageRange, int minValueAmpere, int maxValueAmpere, float electricalOutput, float thermalOutput, float fuelUse, int maxFlowTemperature, int maxReturnTemperature, int warmingPerformanceTolerance, float warmingPerformance100Percent, float warmingPerformance75Percent, float warmingPerformance50Percent, String name) {
        this.percentageRange = percentageRange;
        this.minValueAmpere = minValueAmpere;
        this.maxValueAmpere = maxValueAmpere;
        this.electricalOutput = electricalOutput;
        this.thermalOutput = thermalOutput;
        this.fuelUse = fuelUse;
        this.maxFlowTemperature = maxFlowTemperature;
        this.maxReturnTemperature = maxReturnTemperature;
        this.warmingPerformanceTolerance = warmingPerformanceTolerance;
        this.warmingPerformance50Percent = warmingPerformance50Percent;
        this.warmingPerformance75Percent = warmingPerformance75Percent;
        this.warmingPerformance100Percent = warmingPerformance100Percent;
        this.name = name;

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

    public float getElectricalOutput() {
        return electricalOutput;
    }

    public float getThermalOutput() {
        return thermalOutput;
    }

    public float getFuelUse() {
        return fuelUse;
    }

    public int getWarmingPerformanceTolerance() {
        return warmingPerformanceTolerance;
    }

    public float getWarmingPerformance100Percent() {
        return warmingPerformance100Percent;
    }

    public float getWarmingPerformance75Percent() {
        return warmingPerformance75Percent;
    }

    public float getWarmingPerformance50Percent() {
        return warmingPerformance50Percent;
    }

    public int getMaxFlowTemperature() {
        return maxFlowTemperature;
    }

    public int getMaxReturnTemperature() {
        return maxReturnTemperature;
    }

    public String getName() {
        return name;
    }
}
