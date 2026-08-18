import { FormControl, FormGroup } from "@angular/forms";
import { Subject } from "rxjs";
import { BaseMode, HeatpumpMode, ManualState } from "../shared/shared";
import { ControllerIoHeatpumpBaseModeComponent } from "./basemode";

type BaseModeFormValues = {
    mode: HeatpumpMode;
    manualState: ManualState;
    baseMode: BaseMode;
};

function buildFormAndTriggerBaseMode(
    initial: BaseModeFormValues,
    nextBaseMode: BaseMode | null,
): {
    mode: FormControl<HeatpumpMode | null>;
    manualState: FormControl<ManualState | null>;
    baseMode: FormControl<BaseMode | null>;
} {
    const componentStub: any = {
        getPropertyFromComponent: (key: keyof BaseModeFormValues) => initial[key],
    };

    const stub: any = {
        component: componentStub,
        stopOnDestroy: new Subject<void>(),
    };

    const form = (ControllerIoHeatpumpBaseModeComponent.prototype as any).getFormGroup.call(stub) as FormGroup;
    stub.form = form;

    form.controls["baseMode"].setValue(nextBaseMode);

    return {
        mode: form.controls["mode"] as FormControl<HeatpumpMode | null>,
        manualState: form.controls["manualState"] as FormControl<ManualState | null>,
        baseMode: form.controls["baseMode"] as FormControl<BaseMode | null>,
    };
}

describe("ControllerIoHeatpumpBaseModeComponent", () => {
    describe("#getFormGroup() listener", () => {
        it("returns without changes when baseMode is not set", () => {
            const result = buildFormAndTriggerBaseMode(
                {
                    mode: HeatpumpMode.MANUAL,
                    manualState: ManualState.LOCK,
                    baseMode: BaseMode.FORCE_ON,
                },
                null,
            );

            expect(result.mode.value).toBe(HeatpumpMode.MANUAL);
            expect(result.manualState.value).toBe(ManualState.LOCK);
        });

        it("sets mode to AUTOMATIC when baseMode is AUTOMATIC", () => {
            const result = buildFormAndTriggerBaseMode(
                {
                    mode: HeatpumpMode.MANUAL,
                    manualState: ManualState.LOCK,
                    baseMode: BaseMode.FORCE_ON,
                },
                BaseMode.AUTOMATIC,
            );

            expect(result.mode.value).toBe(HeatpumpMode.AUTOMATIC);
            expect(result.manualState.value).toBe(ManualState.LOCK);
        });

        it("switches to MANUAL and copies manual state for non-AUTOMATIC baseMode", () => {
            const result = buildFormAndTriggerBaseMode(
                {
                    mode: HeatpumpMode.AUTOMATIC,
                    manualState: ManualState.LOCK,
                    baseMode: BaseMode.AUTOMATIC,
                },
                BaseMode.FORCE_ON,
            );

            expect(result.mode.value).toBe(HeatpumpMode.MANUAL);
            expect(result.manualState.value).toBe(ManualState.FORCE_ON);
        });

        it("does not change values when baseMode matches manualState", () => {
            const result = buildFormAndTriggerBaseMode(
                {
                    mode: HeatpumpMode.MANUAL,
                    manualState: ManualState.FORCE_ON,
                    baseMode: BaseMode.AUTOMATIC,
                },
                BaseMode.FORCE_ON,
            );

            expect(result.mode.value).toBe(HeatpumpMode.MANUAL);
            expect(result.manualState.value).toBe(ManualState.FORCE_ON);
        });
    });
});
