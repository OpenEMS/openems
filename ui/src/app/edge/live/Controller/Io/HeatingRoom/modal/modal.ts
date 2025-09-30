// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Component({
    selector: "heatingelement-modal",
    templateUrl: "./MODAL.HTML",
    standalone: false,
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

    protected override getFormGroup(): FormGroup {
        return THIS.FORM_BUILDER.GROUP({
            mode: new FormControl(THIS.COMPONENT.PROPERTIES.MODE),
        });
    }
}
