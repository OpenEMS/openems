// @ts-strict-ignore
import { ChangeDetectorRef, Component, inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: "oe-controller-evse-forecast",
    templateUrl: "./forecast.html",
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
    protected override websocket: Websocket;
    protected override route: ActivatedRoute;
    protected override service: Service;
    override modalController: ModalController;
    protected override translate: TranslateService;
    override formBuilder: FormBuilder;
    override ref: ChangeDetectorRef;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);


    constructor() {
        const websocket = inject<Websocket>(Websocket);
        const route = inject<ActivatedRoute>(ActivatedRoute);
        const service = inject<Service>(Service);
        const modalController = inject<ModalController>(ModalController);
        const translate = inject<TranslateService>(TranslateService);
        const formBuilder = inject<FormBuilder>(FormBuilder);
        const ref = inject(ChangeDetectorRef);

        super(websocket, route, service, modalController, translate, formBuilder, ref);
    
        this.websocket = websocket;
        this.route = route;
        this.service = service;
        this.modalController = modalController;
        this.translate = translate;
        this.formBuilder = formBuilder;
        this.ref = ref;
    }

    override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                res();
            });
        });
    }
}
