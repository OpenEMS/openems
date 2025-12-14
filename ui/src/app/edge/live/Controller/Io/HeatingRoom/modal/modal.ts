// @ts-strict-ignore
import { ChangeDetectorRef, Component, Inject } from "@angular/core";
import { FormBuilder, FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TZDate } from "@date-fns/tz";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { addDays } from "date-fns";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { GetOneTasks } from "./getOneTasks";

@Component({
    selector: "heatingelement-modal",
    templateUrl: "./modal.html",
    standalone: false,
})
export class ModalComponent extends AbstractModal {

    protected oneTasks: GetOneTasks.OneTask[] = [];

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
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                res();
            });
        });
    }

    protected override getFormGroup(): FormGroup {
        return this.formBuilder.group({
            mode: new FormControl(this.component.properties.mode),
        });
    }

    protected override onIsInitialized(): void {
        if (this.component.properties.mode === "AUTOMATIC") {
            const from = new TZDate();
            const to = addDays(from, 2);

            this.edge.sendRequest(this.websocket,
                new ComponentJsonApiRequest({
                    componentId: this.component.id,
                    payload: new GetOneTasks.Request({
                        from: from.toISOString(),
                        to: to.toISOString(),
                    }),
                }))
                .then(response => {
                    this.oneTasks = response.result["oneTasks"] as GetOneTasks.OneTask[];
                })
                .catch(reason => {
                    console.warn(reason);
                    this.oneTasks = [];
                });
        } else {
            this.oneTasks = [];
        }
    }
}
