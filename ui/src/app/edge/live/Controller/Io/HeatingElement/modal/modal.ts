// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { Utils } from "src/app/shared/utils/utils";
import { getInactiveIfPowerIsLow, getRunStateConverter, Level, State, Unit } from "../util/utils";

@Component({
    selector: "heatingelement-modal",
    templateUrl: "./MODAL.HTML",
    standalone: false,
})
export class ModalComponent extends AbstractModal implements OnInit {

    private static PROPERTY_MODE: string = "_PropertyMode";
    private static PREDICTED_PV_PRODUCTION_HOUR = 5;
    private static POWER_OVERSHOOT_FACTOR = 1.1;
    protected readonly CONVERT_HEATING_ELEMENT_RUNSTATE = getRunStateConverter(THIS.TRANSLATE);
    protected mode: string;
    protected runState: State;
    protected level: Level;
    protected consumptionMeter: EDGE_CONFIG.COMPONENT = null;
    protected outputChannelArray: ChannelAddress[] = [];
    protected maxPower: number = 15;
    protected requiredPower: number | null;
    protected isUnreachable: boolean = false;

    protected readonly Mode = Mode;
    protected readonly WorkMode = WorkMode;
    protected readonly Level = Level;

    // TODO remove when outputting of event is errorless possible
    /**
     * An Eventhandler for the toggle
     * @param event the event object from the toogle interaction
     * @param workMode the name of the work mode to activate
     */
    protected switchWorkMode(event, workMode: string): void {
        EVENT.DETAIL.CHECKED ? THIS.FORM_GROUP.CONTROLS["workMode"].setValue(workMode) : THIS.FORM_GROUP.CONTROLS["workMode"].setValue("NONE");
        THIS.FORM_GROUP.CONTROLS["workMode"].markAsDirty();
    }

    protected override onIsInitialized(): void {
        THIS.SUBSCRIPTION.ADD(THIS.FORM_GROUP.GET("minEnergyLimitInKwh").VALUE_CHANGES.SUBSCRIBE((newValue) => {
            THIS.FORM_GROUP.CONTROLS["minEnergylimit"].setValue(newValue * 1000);
            THIS.FORM_GROUP.CONTROLS["minEnergylimit"].markAsDirty();
        }));
    }

    protected getRequiredPower(currentEnergy: number): number | null {
        const now = new Date();
        const timeString = THIS.FORM_GROUP?.controls["endTimeWithMeter"]?.value;
        if (timeString) {
            const [hours, minutes] = TIME_STRING.SPLIT(":").map(Number);
            const endTimeWithMeter = new Date(NOW.GET_FULL_YEAR(), NOW.GET_MONTH(), NOW.GET_DATE(), hours, minutes);
            const energylimit = THIS.FORM_GROUP?.controls["minEnergylimit"]?.value;
            let restEnergy = energylimit - currentEnergy;
            const startTime = new Date(now);

            if (END_TIME_WITH_METER.GET_TIME() < NOW.GET_TIME()) {
                END_TIME_WITH_METER.SET_DATE(END_TIME_WITH_METER.GET_DATE() + 1);
                START_TIME.SET_DATE(NOW.GET_DATE() + 1);
                START_TIME.SET_HOURS(0, 0, 0, 0);
                restEnergy = energylimit;
            }

            return restEnergy / ((END_TIME_WITH_METER.GET_TIME() - START_TIME.GET_TIME()) / 1000 / 3600);
        }
        return null;
    }

    protected pinFormatterEnergy = (value: number) => THIS.PIN_FORMATTER(value, Unit.KILO_WATT_HOURS);
    protected pinFormatterTime = (value: number) => THIS.PIN_FORMATTER(value, UNIT.HOUR);

    protected pinFormatter(value: number, unit: Unit): string {
        switch (unit) {
            case Unit.KILO_WATT_HOURS:
                return Formatter.FORMAT_KILO_WATT_HOURS(value);
            case UNIT.HOUR:
                return Formatter.FORMAT_HOUR(value);
            default:
                return VALUE.TO_STRING();
        }
    }

    protected override getChannelAddresses(): ChannelAddress[] {

        ASSERTION_UTILS.ASSERT_IS_DEFINED<EDGE_CONFIG.COMPONENT>(THIS.COMPONENT, "Heating element can't be found");

        THIS.OUTPUT_CHANNEL_ARRAY.PUSH(
            CHANNEL_ADDRESS.FROM_STRING(
                THIS.COMPONENT.PROPERTIES["outputChannelPhaseL1"]),
            CHANNEL_ADDRESS.FROM_STRING(
                THIS.COMPONENT.PROPERTIES["outputChannelPhaseL2"]),
            CHANNEL_ADDRESS.FROM_STRING(
                THIS.COMPONENT.PROPERTIES["outputChannelPhaseL3"]),
        );

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(THIS.COMPONENT.ID, "ForceStartAtSecondsOfDay"),
            ...THIS.OUTPUT_CHANNEL_ARRAY,
            new ChannelAddress(THIS.COMPONENT.ID, "Level"),
            new ChannelAddress(THIS.COMPONENT.ID, "Status"),
            new ChannelAddress(THIS.COMPONENT.ID, ModalComponent.PROPERTY_MODE),
            new ChannelAddress(THIS.COMPONENT.ID, "_PropertyWorkMode"),

        ];

        if ("METER.ID" in THIS.COMPONENT.PROPERTIES) {
            CHANNEL_ADDRESSES.PUSH(
                new ChannelAddress(THIS.COMPONENT.PROPERTIES["METER.ID"], "ActivePower"),
                new ChannelAddress(THIS.COMPONENT.ID, "Phase1AvgPower"),
                new ChannelAddress(THIS.COMPONENT.ID, "Phase2AvgPower"),
                new ChannelAddress(THIS.COMPONENT.ID, "Phase3AvgPower"),
                new ChannelAddress(THIS.COMPONENT.ID, "SessionEnergy")
            );
        }

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        // get current mode
        ASSERTION_UTILS.ASSERT_IS_DEFINED<EDGE_CONFIG.COMPONENT>(THIS.COMPONENT, "Heating element can't be found");
        THIS.MODE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + ModalComponent.PROPERTY_MODE];
        THIS.CONSUMPTION_METER = THIS.CONFIG.GET_COMPONENT(THIS.COMPONENT.PROPERTIES["METER.ID"]);
        THIS.RUN_STATE = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "Status"];
        THIS.LEVEL = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/" + "Level"];

        if (!THIS.CONSUMPTION_METER) {
            return;
        }

        const activePower = CURRENT_DATA.ALL_COMPONENTS[THIS.CONSUMPTION_METER.ID + "/ActivePower"];

        const avgPowerPhase1 = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Phase1AvgPower"];
        const avgPowerPhase2 = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Phase2AvgPower"];
        const avgPowerPhase3 = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/Phase3AvgPower"];
        const totalPower = UTILS.ADD_SAFELY(avgPowerPhase1, avgPowerPhase2, avgPowerPhase3);

        if (totalPower !== null && totalPower / 1000 * ModalComponent.PREDICTED_PV_PRODUCTION_HOUR > THIS.MAX_POWER) {
            THIS.MAX_POWER = MATH.ROUND(totalPower / 1000) * ModalComponent.PREDICTED_PV_PRODUCTION_HOUR;
        }

        const currentEnergy = CURRENT_DATA.ALL_COMPONENTS[THIS.COMPONENT.ID + "/SessionEnergy"];
        THIS.REQUIRED_POWER = THIS.GET_REQUIRED_POWER(currentEnergy);
        THIS.IS_UNREACHABLE = THIS.REQUIRED_POWER !== null ? THIS.REQUIRED_POWER > totalPower * ModalComponent.POWER_OVERSHOOT_FACTOR : false;
        THIS.RUN_STATE = getInactiveIfPowerIsLow(THIS.RUN_STATE, activePower);
    }

    protected override getFormGroup(): FormGroup {

        const group: FormGroup = THIS.FORM_BUILDER.GROUP({
            minTime: new FormControl(THIS.COMPONENT.PROPERTIES.MIN_TIME),
            minKwh: new FormControl(THIS.COMPONENT.PROPERTIES.MIN_KWH),
            endTime: new FormControl(THIS.COMPONENT.PROPERTIES.END_TIME),
            workMode: new FormControl(THIS.COMPONENT.PROPERTIES.WORK_MODE),
            endTimeWithMeter: new FormControl(THIS.COMPONENT.PROPERTIES.END_TIME_WITH_METER),
            minEnergylimit: new FormControl(THIS.COMPONENT.PROPERTIES.MIN_ENERGYLIMIT),
            minEnergyLimitInKwh: new FormControl(MATH.ROUND(THIS.COMPONENT.PROPERTIES.MIN_ENERGYLIMIT / 1000)),
            defaultLevel: new FormControl(THIS.COMPONENT.PROPERTIES.DEFAULT_LEVEL),
            mode: new FormControl(THIS.MODE),
        });

        if (THIS.COMPONENT.PROPERTIES["METER.ID"] !== null) {
            GROUP.ADD_CONTROL("meterId", new FormControl(THIS.COMPONENT.PROPERTIES["METER.ID"]));
        }

        return group;
    }
}
