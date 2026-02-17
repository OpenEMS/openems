// @ts-strict-ignore
import { AfterViewInit, ChangeDetectorRef, Component, effect, ElementRef, HostListener, Input, Renderer2, untracked } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared";
import { NavigationComponent } from "../action-sheet-modal";
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
export class NavigationPageComponent implements AfterViewInit {

    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup | null = null;

    protected contentHeight: number | null = null;
    protected actionSheetModalHeight: number = 0;

    private edge: Edge | null = null;

    constructor(
        public modalController: ModalController,
        protected service: Service,
        protected navigationService: NavigationService,
        private websocket: Websocket,
        private translate: TranslateService,
        private el: ElementRef,
        private renderer: Renderer2,
        private cdRef: ChangeDetectorRef,
        private routeService: RouteService,
    ) {
        this.service.getCurrentEdge().then(edge => this.edge = edge);
        const hostElement = el.nativeElement;
        this.renderer.addClass(hostElement, "ion-page");

        effect(() => {
            const breakpoint = NavigationComponent.breakPoint();
            if (breakpoint > NavigationComponent.INITIAL_BREAKPOINT) {
                return;
            }
            this.contentHeight = ViewUtils.getViewHeightInPx(untracked(() => this.navigationService.position()));
            this.actionSheetModalHeight = ViewUtils.getActionSheetModalHeightInVh(untracked(() => this.navigationService.position()));
        });
    }

    @HostListener("window:resize", ["$event.target.innerHeight"])
    private onResize(height: number) {
        this.contentHeight = ViewUtils.getViewHeightInPx(untracked(() => this.navigationService.position()));
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
                    this.service.toast(this.translate.instant("GENERAL.CHANGE_ACCEPTED"), "success");
                }).catch(reason => {
                    this.service.toast(this.translate.instant("GENERAL.CHANGE_FAILED") + "\n" + reason.error.message, "danger");
                }).finally(() => this.service.stopSpinner("spinner"));
        }
        this.formGroup.markAsPristine();
    }

    ngAfterViewInit() {
        setTimeout(() => {
            const viewHeight = ViewUtils.getViewHeightInPx(this.navigationService.position());
            this.contentHeight = viewHeight;
        }, 100);
    }

    protected onDomChange() {
        this.contentHeight = ViewUtils.getViewHeightInPx(this.navigationService.position());
    }
}
