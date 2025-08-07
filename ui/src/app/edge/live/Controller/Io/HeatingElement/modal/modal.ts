// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { Utils } from "src/app/shared/utils/utils";
import { getInactiveIfPowerIsLow, getRunStateConverter, Level, State, Unit } from "../util/utils";

@Component({
    selector: "heatingelement-modal",
    templateUrl: "./modal.html",
    standalone: false,
})
export class ModalComponent extends AbstractModal implements OnInit {

    private static PROPERTY_MODE: string = "_PropertyMode";
    private static PREDICTED_PV_PRODUCTION_HOUR = 5;
    private static POWER_OVERSHOOT_FACTOR = 1.1;
    protected readonly CONVERT_HEATING_ELEMENT_RUNSTATE = getRunStateConverter(this.translate);
    protected mode: string;
    protected runState: State;
    protected level: Level;
    protected consumptionMeter: EdgeConfig.Component = null;
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
        event.detail.checked ? this.formGroup.controls["workMode"].setValue(workMode) : this.formGroup.controls["workMode"].setValue("NONE");
        this.formGroup.controls["workMode"].markAsDirty();
    }

    protected override onIsInitialized(): void {
        this.subscription.add(this.formGroup.get("minEnergyLimitInKwh").valueChanges.subscribe((newValue) => {
            this.formGroup.controls["minEnergylimit"].setValue(newValue * 1000);
            this.formGroup.controls["minEnergylimit"].markAsDirty();
        }));
    }

    protected getRequiredPower(currentEnergy: number): number | null {
        const now = new Date();
        const timeString = this.formGroup?.controls["endTimeWithMeter"]?.value;
        if (timeString) {
            const [hours, minutes] = timeString.split(":").map(Number);
            const endTimeWithMeter = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes);
            const energylimit = this.formGroup?.controls["minEnergylimit"]?.value;
            let restEnergy = energylimit - currentEnergy;
            const startTime = new Date(now);

            if (endTimeWithMeter.getTime() < now.getTime()) {
                endTimeWithMeter.setDate(endTimeWithMeter.getDate() + 1);
                startTime.setDate(now.getDate() + 1);
                startTime.setHours(0, 0, 0, 0);
                restEnergy = energylimit;
            }

            return restEnergy / ((endTimeWithMeter.getTime() - startTime.getTime()) / 1000 / 3600);
        }
        return null;
    }

    protected pinFormatterEnergy = (value: number) => this.pinFormatter(value, Unit.KILO_WATT_HOURS);
    protected pinFormatterTime = (value: number) => this.pinFormatter(value, Unit.HOUR);

    protected pinFormatter(value: number, unit: Unit): string {
        switch (unit) {
            case Unit.KILO_WATT_HOURS:
                return Formatter.FORMAT_KILO_WATT_HOURS(value);
            case Unit.HOUR:
                return Formatter.FORMAT_HOUR(value);
            default:
                return value.toString();
        }
    }

    protected override getChannelAddresses(): ChannelAddress[] {

        AssertionUtils.assertIsDefined<EdgeConfig.Component>(this.component, "Heating element can't be found");

        this.outputChannelArray.push(
            ChannelAddress.fromString(
                this.component.properties["outputChannelPhaseL1"]),
            ChannelAddress.fromString(
                this.component.properties["outputChannelPhaseL2"]),
            ChannelAddress.fromString(
                this.component.properties["outputChannelPhaseL3"]),
        );

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(this.component.id, "ForceStartAtSecondsOfDay"),
            ...this.outputChannelArray,
            new ChannelAddress(this.component.id, "Level"),
            new ChannelAddress(this.component.id, "Status"),
            new ChannelAddress(this.component.id, ModalComponent.PROPERTY_MODE),
            new ChannelAddress(this.component.id, "_PropertyWorkMode"),

        ];

        if ("meter.id" in this.component.properties) {
            channelAddresses.push(
                new ChannelAddress(this.component.properties["meter.id"], "ActivePower"),
                new ChannelAddress(this.component.id, "Phase1AvgPower"),
                new ChannelAddress(this.component.id, "Phase2AvgPower"),
                new ChannelAddress(this.component.id, "Phase3AvgPower"),
                new ChannelAddress(this.component.id, "SessionEnergy")
            );
        }

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        // get current mode
        AssertionUtils.assertIsDefined<EdgeConfig.Component>(this.component, "Heating element can't be found");
        this.mode = currentData.allComponents[this.component.id + "/" + ModalComponent.PROPERTY_MODE];
        this.consumptionMeter = this.config.getComponent(this.component.properties["meter.id"]);
        this.runState = currentData.allComponents[this.component.id + "/" + "Status"];
        this.level = currentData.allComponents[this.component.id + "/" + "Level"];

        if (!this.consumptionMeter) {
            return;
        }

        const activePower = currentData.allComponents[this.consumptionMeter.id + "/ActivePower"];

        const avgPowerPhase1 = currentData.allComponents[this.component.id + "/Phase1AvgPower"];
        const avgPowerPhase2 = currentData.allComponents[this.component.id + "/Phase2AvgPower"];
        const avgPowerPhase3 = currentData.allComponents[this.component.id + "/Phase3AvgPower"];
        const totalPower = Utils.addSafely(avgPowerPhase1, avgPowerPhase2, avgPowerPhase3);

        if (totalPower !== null && totalPower / 1000 * ModalComponent.PREDICTED_PV_PRODUCTION_HOUR > this.maxPower) {
            this.maxPower = Math.round(totalPower / 1000) * ModalComponent.PREDICTED_PV_PRODUCTION_HOUR;
        }

        const currentEnergy = currentData.allComponents[this.component.id + "/SessionEnergy"];
        this.requiredPower = this.getRequiredPower(currentEnergy);
        this.isUnreachable = this.requiredPower !== null ? this.requiredPower > totalPower * ModalComponent.POWER_OVERSHOOT_FACTOR : false;
        this.runState = getInactiveIfPowerIsLow(this.runState, activePower);
    }

    protected override getFormGroup(): FormGroup {

        const group: FormGroup = this.formBuilder.group({
            minTime: new FormControl(this.component.properties.minTime),
            minKwh: new FormControl(this.component.properties.minKwh),
            endTime: new FormControl(this.component.properties.endTime),
            workMode: new FormControl(this.component.properties.workMode),
            endTimeWithMeter: new FormControl(this.component.properties.endTimeWithMeter),
            minEnergylimit: new FormControl(this.component.properties.minEnergylimit),
            minEnergyLimitInKwh: new FormControl(Math.round(this.component.properties.minEnergylimit / 1000)),
            defaultLevel: new FormControl(this.component.properties.defaultLevel),
            mode: new FormControl(this.mode),
        });

        if (this.component.properties["meter.id"] !== null) {
            group.addControl("meterId", new FormControl(this.component.properties["meter.id"]));
        }

        return group;
    }
}
