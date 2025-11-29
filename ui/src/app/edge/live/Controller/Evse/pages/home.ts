// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
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

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
        private navigationService: NavigationService,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
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
