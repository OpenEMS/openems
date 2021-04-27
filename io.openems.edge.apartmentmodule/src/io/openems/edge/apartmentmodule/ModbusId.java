package io.openems.edge.apartmentmodule;

public enum ModbusId {
    ID_1(1, "1 - Bottom"), //
    ID_2(2, "2 - Top"), //
    ID_3(3, "3 - Top"), //
    ID_4(4, "4 - Bottom"), //
    ID_5(5, "5 - Bottom"); //

    private int value;
    private String name;

    private ModbusId(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

}