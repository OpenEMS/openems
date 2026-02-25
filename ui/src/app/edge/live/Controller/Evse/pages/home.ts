// @ts-strict-ignore
import { Component, inject } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetOneTasks } from "src/app/shared/jsonrpc/request/getOneTasks";
import { GetOneTasksResponse } from "src/app/shared/jsonrpc/response/getOneTasksResponse";
import { EdgeConfig, EdgePermission } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { EvseChargepoint } from "../shared/evse-chargepoint";
import { ControllerEvseSingleShared } from "../shared/shared";

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

    protected showNewFooter: boolean = true;
    protected label: string | null = null;
    protected chargePointComponent: EdgeConfig.Component | null = null;

    protected img: OeImageComponent["img"] | null = null;

    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);
    protected readonly CONVERT_TO_STATE_MACHINE_LABEL = ControllerEvseSingleShared.CONVERT_TO_STATE_MACHINE_LABEL(this.translate);
    protected readonly CONVERT_TO_ACTUAL_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_ACTUAL_MODE_LABEL(this.translate);
    protected readonly CONVERT_TO_PHASE_SWITCH_LABEL = ControllerEvseSingleShared.CONVERT_TO_PHASE_SWITCH_LABEL(this.translate);
    protected readonly CONVERT_TO_ENERGY_LIMIT_LABEL = ControllerEvseSingleShared.CONVERT_TO_ENERGY_LIMIT_LABEL();
    protected oneTasks: OneTaskVM[] = null;
    private navigationService: NavigationService = inject(NavigationService);


    async ionViewWillEnter() {
        const edge = await this.service.getCurrentEdge();
        const config = await this.service.getConfig();

        if (edge == null || config == null) {
            return;
        }

        this.chargePointComponent = config.getComponentFromOtherComponentsProperty(this.component.id, "chargePoint.id") ?? null;
        if (EdgePermission.hasPhaseSwitchingAbility(edge, this.chargePointComponent) === false) {
            return;
        }

        const tree = new NavigationTree("phase-switching", { baseString: "phase-switching" }, { name: "menu-outline", color: "warning" }, this.translate.instant("EDGE.INDEX.WIDGETS.EVCS.PHASE_SWITCHING"), "label", [], null);
        this.navigationService.setChildToCurrentNavigation(tree);
    }

    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                res();
            });
        });
    }

    protected override onIsInitialized(): void {
        this.chargePointComponent = this.config.getComponentFromOtherComponentsProperty(this.component.id, "chargePoint.id") ?? null;

        const evseChargepoint: EvseChargepoint | null = EvseChargepoint.getEvseChargepoint(this.chargePointComponent);
        if (evseChargepoint == null || this.chargePointComponent == null) {
            return;
        }
        // Current date/time
        const now = new Date(Date.now());

        // Three days from now
        const threeDaysFromNow = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000); // 3 days in milliseconds

        this.img = evseChargepoint.img;
        this.edge.sendRequest(this.websocket, new ComponentJsonApiRequest({
            componentId: this.component.id,
            payload: new GetOneTasks(now.toISOString(), threeDaysFromNow.toISOString()),
        })).then(response => {
            const resp = response as GetOneTasksResponse;
            this.oneTasks = resp.result.oneTasks.map(item => ({
                start: item.start.replace(/([+-]\d{2}:\d{2}|Z)$/, ""),
                end: item.end.replace(/([+-]\d{2}:\d{2}|Z)$/, ""),
                mode: this.CONVERT_TO_MODE_LABEL(item.payload.mode),
            }));
        });
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

interface OneTaskVM {
    start: string;
    end: string;
    mode: string;
};
