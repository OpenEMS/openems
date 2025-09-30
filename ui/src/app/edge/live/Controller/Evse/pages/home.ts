// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";
import { ControllerEvseSingleShared } from "../shared/shared";

@Component({
    selector: "oe-controller-evse-single-home",
    templateUrl: "./HOME.HTML",
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
    protected chargePoint: EDGE_CONFIG.COMPONENT | null = null;

    protected img: OeImageComponent["img"] | null = null;

    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(THIS.TRANSLATE);

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
    }

    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            THIS.ROUTE.PARAMS.PIPE(filter(params => params != null), take(1)).subscribe((params) => {
                THIS.COMPONENT = CONFIG.GET_COMPONENT(PARAMS.COMPONENT_ID);
                THIS.CHARGE_POINT = CONFIG.GET_COMPONENT_FROM_OTHER_COMPONENTS_PROPERTY(THIS.COMPONENT.ID, "CHARGE_POINT.ID") ?? null;
                res();
            });
        });
    }

    protected override onIsInitialized(): void {
        const url = CONTROLLER_EVSE_SINGLE_SHARED.GET_IMG_URL_BY_FACTORY_ID(THIS.CHARGE_POINT.FACTORY_ID);
        THIS.IMG = url === null ? null : { url, height: 300, width: 300 };
    }

    protected override getFormGroup(): FormGroup {
        ASSERTION_UTILS.ASSERT_IS_DEFINED(THIS.COMPONENT);
        return THIS.FORM_BUILDER.GROUP({
            mode: new FormControl(THIS.COMPONENT.PROPERTIES.MODE),
        });
    }

    protected hideFooter() {
        THIS.SHOW_NEW_FOOTER = !THIS.SHOW_NEW_FOOTER;
    }
}
