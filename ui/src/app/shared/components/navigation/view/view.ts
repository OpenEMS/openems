// @ts-strict-ignore
import { Component, effect, Input, untracked, inject } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared";
import { NavigationComponent } from "../navigation.component";
import { NavigationService } from "../service/navigation.service";
import { ViewUtils } from "./shared/shared";

export enum Status {
    SUCCESS,
    ERROR,
    PENDING,
}

/**
 * Always use conditionally rendering, this component doesnt wait for async events to be resolved first
 */
@Component({
    selector: "oe-navigation-view",
    templateUrl: "./view.html",
    styles: [`
        :host {
            height: 100%;
            font-size: 0.9em;

            ion-grid {
                display: inline !important;
            }
        }
    `],
    standalone: false,
})
export class NavigationPageComponent {
    modalController = inject(ModalController);
    protected service = inject(Service);
    protected navigationService = inject(NavigationService);
    private websocket = inject(Websocket);
    private translate = inject(TranslateService);


    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup = new FormGroup({});

    protected contentHeight: number | null = null;

    private edge: Edge | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        this.service.getCurrentEdge().then(edge => this.edge = edge);

        effect(() => {
            const breakpoint = NavigationComponent.breakPoint();
            if (breakpoint > NavigationComponent.INITIAL_BREAKPOINT) {
                return;
            }
            this.contentHeight = ViewUtils.getViewHeight(untracked(() => this.navigationService.position()));
        });
    }

    // Changes applied together
    public applyChanges() {
        const updateComponentArray: { name: string, value: any }[] = [];
        this.service.startSpinner("spinner");
        for (const key in this.formGroup.controls) {
            const control = this.formGroup.controls[key];
            this.formGroup.controls[key];

            // Check if formControl-value didn't change
            if (control.pristine) {
                continue;
            }

            updateComponentArray.push({
                name: key,
                value: this.formGroup.value[key],
            });
        }

        if (this.edge) {
            this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray)
                .then(() => {
                    this.service.toast(this.translate.instant("General.changeAccepted"), "success");
                }).catch(reason => {
                    this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
                }).finally(() => this.service.stopSpinner("spinner"));
        }
        this.formGroup.markAsPristine();
    }

    protected onDomChange() {
        this.contentHeight = ViewUtils.getViewHeight(this.navigationService.position());
    }
}
