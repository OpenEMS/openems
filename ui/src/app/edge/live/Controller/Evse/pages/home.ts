// @ts-strict-ignore
import { ChangeDetectorRef, Component, inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { OeImageComponent } from "src/app/shared/components/oe-img/oe-img";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
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
    protected override websocket: Websocket;
    protected override route: ActivatedRoute;
    protected override service: Service;
    override modalController: ModalController;
    protected override translate: TranslateService;
    override formBuilder: FormBuilder;
    override ref: ChangeDetectorRef;


    protected showNewFooter: boolean = true;
    protected label: string | null = null;
    protected chargePoint: EdgeConfig.Component | null = null;

    protected img: OeImageComponent["img"] | null = null;

    protected readonly CONVERT_TO_MODE_LABEL = ControllerEvseSingleShared.CONVERT_TO_MODE_LABEL(this.translate);

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

    public override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                this.chargePoint = config.getComponentFromOtherComponentsProperty(this.component.id, "chargePoint.id") ?? null;
                res();
            });
        });
    }

    protected override onIsInitialized(): void {
        const url = ControllerEvseSingleShared.getImgUrlByFactoryId(this.chargePoint.factoryId);
        this.img = url === null ? null : { url, height: 300, width: 300 };
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
