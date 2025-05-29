// @ts-strict-ignore
import { AfterViewChecked, Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared";
import { NavigationComponent } from "../navigation.component";
import { NavigationService } from "../service/navigation.service";

export enum Status {
    SUCCESS,
    ERROR,
    PENDING,
}

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
export class NavigationPageComponent implements AfterViewChecked {

    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup = new FormGroup({});

    protected contentHeight: number | null = null;

    private edge: Edge | null = null;

    constructor(
        public modalController: ModalController,
        private websocket: Websocket,
        private service: Service,
        protected navigationService: NavigationService,
        private translate: TranslateService,
    ) {
        this.service.getCurrentEdge().then(edge => this.edge = edge);
    }


    ngAfterViewChecked() {
        this.contentHeight = this.calculateHeight();
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

    private calculateHeight(): number {

        // !IMPORTANT TODO: Calculate container height
        return 100 - (this.navigationService.position == "bottom" ? (NavigationComponent.INITIAL_BREAKPOINT * 100) : 5);
    }
}

