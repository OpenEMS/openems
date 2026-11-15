import { ChangeDetectionStrategy, Component, OnInit } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ToggleChangeEventDetail } from "@ionic/core";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { Utils } from "src/app/shared/utils/utils";
import { SharedControllerIoHeatingElement } from "../shared/shared";
import { getInactiveIfPowerIsLow, getRunStateConverter, HeatingElementComponent, Level, State, Unit, } from "../util/utils";

@Component({
    selector: "heatingelement-modal",
    templateUrl: "./modal.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ModalComponent extends AbstractModal implements OnInit {
    private static PROPERTY_MODE: string = "_PropertyMode";
    private heatingElementConfig: HeatingElementComponent | null = null;
    private levelOptions: { name: string; value: string }[] = [];
    private modeButtons: ButtonLabel[] = [];
    private readonly CONVERT_HEATING_ELEMENT_RUNSTATE = getRunStateConverter(this.translate);
    private mode: string | null = null;
    private runState: State = State.UNDEFINED;
    private level: Level = Level.LEVEL_0;
    private consumptionMeter: EdgeConfig.Component | null = null;
    private outputChannelArray: ChannelAddress[] = [];
    private maxPower: number = 15;
    private requiredPower: number | null = null;
    private isUnreachable: boolean = false;

    private readonly Mode = Mode;
    private readonly WorkMode = WorkMode;
    private readonly Level = Level;

    protected override onIsInitialized(): void {
        AssertionUtils.assertIsDefined(this.formGroup, "Heating element form is not initialized");
        const minEnergyLimitInKwh = this.formGroup.get("minEnergyLimitInKwh");
        const minEnergylimit = this.formGroup.get("minEnergylimit");
        AssertionUtils.assertIsDefined(minEnergyLimitInKwh, "Heating element energy limit control is missing");
        AssertionUtils.assertIsDefined(minEnergylimit, "Heating element energy limit control is missing");
        this.heatingElementConfig = new HeatingElementComponent(this.component, this.edge);
        this.updateTranslatedOptions();

        this.subscription.add(
            this.translate.onLangChange.subscribe(() => {
                this.updateTranslatedOptions();
            }),
        );
        this.subscription.add(
            minEnergyLimitInKwh.valueChanges.subscribe((newValue) => {
                minEnergylimit.setValue(newValue * 1000);
                minEnergylimit.markAsDirty();
            }),
        );
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        AssertionUtils.assertIsDefined(this.component, "Heating element can't be found");

        this.outputChannelArray.push(
            ChannelAddress.fromString(this.component.properties["outputChannelPhaseL1"]),
            ChannelAddress.fromString(this.component.properties["outputChannelPhaseL2"]),
            ChannelAddress.fromString(this.component.properties["outputChannelPhaseL3"]),
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
                new ChannelAddress(this.component.id, "SessionEnergy"),
            );
        }

        return channelAddresses;
    }

    protected override onCurrentData(currentData: CurrentData) {
        // get current mode
        AssertionUtils.assertIsDefined(this.component, "Heating element can't be found");
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

        const powerInKw = NumberUtils.divideSafely(totalPower, 1000);
        if (powerInKw != null) {
            const predictedEnergy = NumberUtils.multiplySafely(
                Math.round(powerInKw),
                SharedControllerIoHeatingElement.PREDICTED_PV_PRODUCTION_HOUR,
            );
            if (predictedEnergy != null && predictedEnergy > this.maxPower) {
                this.maxPower = predictedEnergy;
            }
        }

        const currentEnergy = currentData.allComponents[this.component.id + "/SessionEnergy"];
        this.requiredPower = this.getRequiredPower(currentEnergy);
        this.isUnreachable =
            this.requiredPower !== null &&
            this.requiredPower > (totalPower ?? 0) * SharedControllerIoHeatingElement.POWER_OVERSHOOT_FACTOR;
        this.runState = getInactiveIfPowerIsLow(this.runState, activePower);
    }

    protected override getFormGroup(): FormGroup {
        AssertionUtils.assertIsDefined(this.component, "Heating element can't be found");
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

    private pinFormatterEnergy = (value: number) => this.pinFormatter(value, Unit.KILO_WATT_HOURS);
    private pinFormatterTime = (value: number) => this.pinFormatter(value, Unit.HOUR);

    private switchWorkMode(event: CustomEvent<ToggleChangeEventDetail>, workMode: string): void {
        AssertionUtils.assertIsDefined(this.formGroup, "Heating element form is not initialized");
        this.formGroup.controls["workMode"].setValue(event.detail.checked ? workMode : "NONE");
        this.formGroup.controls["workMode"].markAsDirty();
    }

    private getRequiredPower(currentEnergy: number): number | null {
        if (this.formGroup == null) {
            return null;
        }

        const now = new Date();
        const timeString = FormUtils.findFormControlsValueSafely<string | null>(this.formGroup, "endTimeWithMeter");
        const energyLimit = FormUtils.findFormControlsValueSafely<number | null>(this.formGroup, "minEnergylimit");
        if (timeString == null || energyLimit == null) {
            return null;
        }
        if (typeof timeString !== "string" || timeString === "" || !Number.isFinite(energyLimit)) {
            return null;
        }

        const [hours, minutes] = timeString.split(":").map(Number);
        const endTimeWithMeter = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes);
        let restEnergy = energyLimit - currentEnergy;
        const startTime = new Date(now);

        if (endTimeWithMeter.getTime() < now.getTime()) {
            endTimeWithMeter.setDate(endTimeWithMeter.getDate() + 1);
            startTime.setDate(now.getDate() + 1);
            startTime.setHours(0, 0, 0, 0);
            restEnergy = energyLimit;
        }

        return restEnergy / ((endTimeWithMeter.getTime() - startTime.getTime()) / 1000 / 3600);
    }

    private pinFormatter(value: number, unit: Unit): string {
        switch (unit) {
            case Unit.KILO_WATT_HOURS:
                return Formatter.FORMAT_KILO_WATT_HOURS(value);
            case Unit.HOUR:
                return Formatter.FORMAT_HOUR(value);
            default:
                return value.toString();
        }
    }

    private updateTranslatedOptions(): void {
        if (this.heatingElementConfig == null) {
            return;
        }
        this.levelOptions = this.heatingElementConfig.getLevelOptions(this.translate);
        this.modeButtons = this.heatingElementConfig.getModeButtons(this.translate);
    }
}
