import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { FormUtils } from "./FORM.UTILS";

describe("FormUtils", () => {

    it("#findFormControlSafely - control not found", () => {
        const formGroup: FormGroup = new FormGroup({});
        expect(FORM_UTILS.FIND_FORM_CONTROL_SAFELY(formGroup, "control")).toEqual(null);
    });

    it("#findFormControlSafely - control found", () => {
        const formGroup: FormGroup = new FormGroup({
            "control": new FormControl(),
        });
        expect(FORM_UTILS.FIND_FORM_CONTROL_SAFELY(formGroup, "control")).toEqual(FORM_GROUP.CONTROLS["control"]);
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
        expect(FORM_UTILS.FIND_FORM_CONTROL_SAFELY(formGroup, "control")).toEqual(((FORM_GROUP.CONTROLS["some"] as FormGroup).controls["some"] as FormGroup).controls["control"]);
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
        expect(FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<number>(formGroup, "control")).toEqual(1000);
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
        expect(FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY<number>(formGroup, "control")).toEqual(null);
    });
});
