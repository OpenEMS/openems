import { Component, model } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { EvseChargepoint } from "../shared/evse-chargepoint";
import { ControllerEvseSingleShared } from "../shared/shared";
import { EvseManualPayload } from "./schedule/js-calender-utils";

@Component({
    selector: "oe-controller-evse-single-home",
    templateUrl: "./home.html",
    standalone: false,
    styles: [
        `
        .oe-modal-buttons-text-size {
            form ion-segment ion-segment-button ion-label {
                white-space: pre-wrap;
                font-size: x-small !important;
            }
        }
        `,
    ],
})
export class ModalComponent extends AbstractModal {

    public payload = model(new EvseManualPayload());
    protected showNewFooter: boolean = true;
    protected label: string | null = null;
    protected chargePointComponent: EdgeConfig.Component | null = null;

    protected img: OeImageComponent["img"] | null = null;

    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);
    protected readonly CONVERT_TO_STATE_MACHINE_LABEL = ControllerEvseSingleShared.CONVERT_TO_STATE_MACHINE_LABEL(this.translate);
    protected readonly CONVERT_TO_ACTUAL_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_ACTUAL_MODE_LABEL(this.translate);
    protected readonly CONVERT_TO_PHASE_SWITCH_LABEL = ControllerEvseSingleShared.CONVERT_TO_PHASE_SWITCH_LABEL(this.translate);
    protected readonly CONVERT_TO_ENERGY_LIMIT_LABEL = ControllerEvseSingleShared.CONVERT_TO_ENERGY_LIMIT_LABEL();
    protected oneTasks: OneTaskVM[] = [];

    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                res();
            });
        });
    }

    protected override onIsInitialized(): void {
        AssertionUtils.assertIsDefined(this.component);
        this.chargePointComponent = this.config.getComponentFromOtherComponentsProperty(this.component.id, "chargePoint.id") ?? null;

        const evseChargepoint: EvseChargepoint | null = EvseChargepoint.getEvseChargepoint(this.chargePointComponent);
        if (evseChargepoint == null || this.chargePointComponent == null) {
            return;
        }

        this.img = evseChargepoint.img;
    }

    protected override getFormGroup(): FormGroup {
        AssertionUtils.assertIsDefined(this.component);
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode),
        });
    }

    protected hideFooter() {
        this.showNewFooter = !this.showNewFooter;
    }
}

export interface OneTaskVM {
    start: string;
    end: string;
    mode: string;
};
