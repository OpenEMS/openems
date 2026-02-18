import { FormControl, Validators } from "@angular/forms";
import { FormlyUtils } from "./formly-utils";

describe("formly-utils.ts", () => {

    it("should return the INVALID color when the control is invalid and touched", () => {
        const control = new FormControl("", Validators.required);
        control.markAsTouched(); // Control is now invalid and touched

        const style = FormlyUtils.getControlStyle(control, false, "border-bottom-color");

        expect(style["border-bottom-color"]).toBe("var(--ion-color-danger)");
    });

    it("should return the VALID color when the control is valid", () => {
        const control = new FormControl("some-value", Validators.required);
        control.markAsTouched();

        const style = FormlyUtils.getControlStyle(control, false, "border-bottom-color");
        expect(style["border-bottom-color"]).toBe("var(--ion-color-success)");
    });

    it("should return the FOCUSED color when the control is focused but not invalid/touched or valid", () => {
        const control = new FormControl("", Validators.required); // Pristine and untouched
        const style = FormlyUtils.getControlStyle(control, true, "border-bottom-color");

        expect(style["border-bottom-color"]).toBe("var(--ion-color-primary)");
    });

    it("should return the DEFAULT color for a pristine, untouched, and unfocused control", () => {
        const control = new FormControl("", Validators.required); // Pristine and untouched
        const style = FormlyUtils.getControlStyle(control, false, "border-bottom-color");

        expect(style["border-bottom-color"]).toBe("var(--ion-color-dark)");
    });

    it("should prioritize the INVALID color over the FOCUSED color", () => {
        const control = new FormControl("", Validators.required);
        control.markAsTouched(); // Invalid and touched

        const style = FormlyUtils.getControlStyle(control, true, "border-bottom-color");
        expect(style["border-bottom-color"]).toBe("var(--ion-color-danger)");
    });
});
