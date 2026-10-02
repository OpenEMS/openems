import { FormControl, FormGroup } from "@angular/forms";

import { TimeLineComponent } from "./picktime.component";

describe("Picktime", () => {
    let component: TimeLineComponent;
    let formGroup: FormGroup;

    beforeEach(() => {
        component = new TimeLineComponent();

        formGroup = new FormGroup({
            startTime: new FormControl("08:00"),
        });

        component.formGroup = formGroup;
        component.controlName = "startTime";
    });

    it("#onTimeChange - should update form control value", () => {
        component.onTimeChange("12:30");

        expect(formGroup.get("startTime")?.value).toBe("12:30");
    });

    it("#onTimeChange - should mark form control as dirty", () => {
        const control = formGroup.get("startTime");

        expect(control?.dirty).toBe(false);

        component.onTimeChange("12:30");

        expect(control?.dirty).toBe(true);
    });

    it("#onTimeChange - should ignore null", () => {
        const control = formGroup.get("startTime");

        component.onTimeChange(null);

        expect(control?.value).toBe("08:00");
        expect(control?.dirty).toBe(false);
    });

    it("#onTimeChange - should not throw if control does not exist", () => {
        component.controlName = "unknownControl";

        expect(() => component.onTimeChange("12:30")).not.toThrow();
    });
});
