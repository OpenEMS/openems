import { AfterViewInit, ChangeDetectorRef, Component, effect, ElementRef, EventEmitter, HostListener, input, Input, Output, Renderer2, signal, untracked } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared";
import { HelpButtonComponent } from "../../modal/help-button/help-button";
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
            .floating-btn {
                position: fixed;
                right: 10%;
            }
        }
    `],
    standalone: false,
})
export class NavigationPageComponent implements AfterViewInit {

    @Output() public emitForm: EventEmitter<FormGroup | null> = new EventEmitter();
    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup | null = null;

    protected helpKey = input<HelpButtonComponent["key"]>();
    protected bottomPx = signal(0);

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
        this.emitForm.emit(this.formGroup ?? null);
        return;
    }

    ngAfterViewInit() {
        setTimeout(() => {
            const viewHeight = ViewUtils.getViewHeightInPx(this.navigationService.position());
            this.contentHeight = viewHeight;
        }, 100);
        this.bottomPx.set(ViewUtils.getActionSheetModalHeightInPx());
    }

    protected onDomChange() {
        this.contentHeight = ViewUtils.getViewHeightInPx(this.navigationService.position());
    }
}
