// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject, ViewEncapsulation } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { NavigationService } from "src/app/shared/components/navigation/service/NAVIGATION.SERVICE";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";

@Component({
    selector: "oe-controller-evse-history",
    templateUrl: "./HISTORY.HTML",
    standalone: false,
    styles: [
        `
       form {
            align-content: center !important;
        }
        `,
    ],
    encapsulation: VIEW_ENCAPSULATION.NONE,
})
export class ModalComponent extends AbstractModal {

    protected showNewFooter: boolean = true;
    protected showStatusChart: boolean = false;
    protected label: string | null = null;
    protected meterId: string | null = null;

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
        protected navigationService: NavigationService,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
    }

    override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            THIS.ROUTE.PARAMS.PIPE(filter(params => params != null), take(1)).subscribe((params) => {
                THIS.COMPONENT = CONFIG.GET_COMPONENT(PARAMS.COMPONENT_ID);
                THIS.METER_ID = THIS.CONFIG.GET_COMPONENT_FROM_OTHER_COMPONENTS_PROPERTY(THIS.COMPONENT.ID, "CHARGE_POINT.ID")?.id ?? null;
                const timeOfUseCtrl = THIS.CONFIG.GET_COMPONENTS_BY_FACTORY("CONTROLLER.ESS.TIME-Of-Use-Tariff")?.[0] ?? null;
                THIS.SHOW_STATUS_CHART = timeOfUseCtrl !== null;
                res();
            });
        });
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
