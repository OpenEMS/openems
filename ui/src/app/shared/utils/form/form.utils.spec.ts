import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { FormUtils } from "./form.utils";

describe("FormUtils", () => {

    it("#findFormControlSafely - control not found", () => {
        const formGroup: FormGroup = new FormGroup({});
        expect(FormUtils.findFormControlSafely(formGroup, "control")).toEqual(null);
    });

    it("#findFormControlSafely - control found", () => {
        const formGroup: FormGroup = new FormGroup({
            "control": new FormControl(),
        });
        expect(FormUtils.findFormControlSafely(formGroup, "control")).toEqual(formGroup.controls["control"]);
    });

    it("#findFormControlSafely - control nested deeper found", () => {
        const formGroup: FormGroup = new FormGroup({
            "some": new FormBuilder().group({
                "some": new FormBuilder().group({
                    "something": new FormControl(),
                    "control": new FormControl(1000),
                }),
            }),
        });
        expect(FormUtils.findFormControlSafely(formGroup, "control")).toEqual(((formGroup.controls["some"] as FormGroup).controls["some"] as FormGroup).controls["control"]);
    });

    it("#findFormControlsValueSafely - control nested deeper found", () => {
        const formGroup: FormGroup = new FormGroup({
            "some": new FormBuilder().group({
                "some": new FormBuilder().group({
                    "something": new FormControl(),
                    "control": new FormControl(1000),
                }),
            }),
        });
        expect(FormUtils.findFormControlsValueSafely<number>(formGroup, "control")).toEqual(1000);
    });

    it("#findFormControlsValueSafely - control nested deeper not found", () => {
        const formGroup: FormGroup = new FormGroup({
            "some": new FormBuilder().group({
                "some": new FormBuilder().group({
                    "something": new FormControl(),
                    "control2": new FormControl(1000),
                }),
            }),
        });
        expect(FormUtils.findFormControlsValueSafely<number>(formGroup, "control")).toEqual(null);
    });
});
