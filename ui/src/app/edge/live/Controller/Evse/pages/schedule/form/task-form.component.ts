import { Component, model } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { ControllerEvseSingleShared } from "../../../shared/shared";
import { Mode } from "../../chargemode/chargemode";

@Component({
    selector: "task-form",
    templateUrl: "./task-form.component.html",
    standalone: false,
})
export class TaskFormComponent {
    protected modes = Object.values(Mode);
    protected modeOptions: { value: Mode, label: string }[] = [];

    protected startTime = model<string | null>(null);
    protected endTime = model<string | null>(null);
    protected selectedMode = model<Mode | null>(null);

    constructor(private translate: TranslateService) {
        const convert = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);
        this.modeOptions = this.modes.map(mode => ({
            value: mode,
            label: convert(mode),
        }));
    }
}
