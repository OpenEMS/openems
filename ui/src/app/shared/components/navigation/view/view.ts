// @ts-strict-ignore
import { ChangeDetectorRef, Component, effect, Input } from "@angular/core";
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
export class NavigationPageComponent {

    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup = new FormGroup({});

    protected contentHeight: number | null = null;

    private edge: Edge | null = null;

    constructor(
        public modalController: ModalController,
        protected service: Service,
        protected navigationService: NavigationService,
        private websocket: Websocket,
        private translate: TranslateService,
        private cdr: ChangeDetectorRef
    ) {
        this.service.getCurrentEdge().then(edge => this.edge = edge);

        effect(() => {
            const position = this.navigationService.position();
            this.contentHeight = NavigationPageComponent.calculateHeight(position);
        });
    }

    public static calculateHeight(position: string | null): number {
        if (position == null) {
            return 100;
        }

        const HEADER_HEIGHT_WITH_PICKDATE_BREADCRUMBS = 10;
        if (position === "bottom") {
            return 100 - ((NavigationComponent.INITIAL_BREAKPOINT * 100) + HEADER_HEIGHT_WITH_PICKDATE_BREADCRUMBS);
        }

        // !IMPORTANT TODO: Calculate container height dynamically
        return 100 - ((NavigationComponent.INITIAL_BREAKPOINT * 100));
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
}

