// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: "oe-controller-evse-forecast",
    templateUrl: "./FORECAST.HTML",
    standalone: false,
    styles: [
        `
        .ion-justify-with-space-between{
            ion-row > ion-col:nth-child(2){
                text-align: right;
            }
        }

         form {
            align-content: center !important;
        }
        `,
    ],
})
export class ModalComponent extends AbstractModal {

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

    override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            THIS.ROUTE.PARAMS.PIPE(filter(params => params != null), take(1)).subscribe((params) => {
                THIS.COMPONENT = CONFIG.GET_COMPONENT(PARAMS.COMPONENT_ID);
                res();
            });
        });
    }
}
