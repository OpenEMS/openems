import { FormControl, Validators } from "@angular/forms";
import { FormlyUtils } from "./formly-utils";

describe("formly-utils.ts", () => {

    it("should return the INVALID color when the control is invalid and touched", () => {
        const control = new FormControl("", Validators.required);
        control.markAsTouched(); // Control is now invalid and touched

        const style = FormlyUtils.getBorderBottomColor(control, false);

        expect(style["border-bottom-color"]).toBe("var(--highlight-color-invalid)");
    });

    it("should return the VALID color when the control is valid", () => {
        const control = new FormControl("some-value", Validators.required);
        control.markAsTouched();

        const style = FormlyUtils.getBorderBottomColor(control, false);

        expect(style["border-bottom-color"]).toBe("var(--highlight-color-valid)");
    });

    it("should return the FOCUSED color when the control is focused but not invalid/touched or valid", () => {
        const control = new FormControl("", Validators.required); // Pristine and untouched
        const style = FormlyUtils.getBorderBottomColor(control, true);

        expect(style["border-bottom-color"]).toBe("var(--highlight-color-focused)");
    });

    it("should return the DEFAULT color for a pristine, untouched, and unfocused control", () => {
        const control = new FormControl("", Validators.required); // Pristine and untouched
        const style = FormlyUtils.getBorderBottomColor(control, false);

        expect(style["border-bottom-color"]).toBe("var(--ion-color-dark)");
    });

    it("should prioritize the INVALID color over the FOCUSED color", () => {
        const control = new FormControl("", Validators.required);
        control.markAsTouched(); // Invalid and touched

        const style = FormlyUtils.getBorderBottomColor(control, true);

        expect(style["border-bottom-color"]).toBe("var(--highlight-color-invalid)");
    });
});
