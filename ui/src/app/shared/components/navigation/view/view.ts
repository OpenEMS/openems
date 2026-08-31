import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, input, Input, Output, Renderer2, } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { EdgeConfig, Service } from "../../../shared";
import { HelpButtonComponent } from "../../modal/help-button/help-button";
import { NavigationService } from "../service/navigation.service";

export enum Status {
    SUCCESS,
    ERROR,
    PENDING,
}

/** Always use conditionally rendering, this component doesnt wait for async events to be resolved first */
@Component({
    selector: "oe-navigation-view",
    templateUrl: "./view.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
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
            .floating-btn {
                position: fixed;
                right: 5%;
            }
        `,
    ],
    standalone: false,
})
export class NavigationPageComponent {
    @Output() public emitForm: EventEmitter<FormGroup | null> = new EventEmitter();
    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup | null = null;

    protected helpKey = input<HelpButtonComponent["key"]>();

    constructor(
        public modalController: ModalController,
        protected service: Service,
        protected navigationService: NavigationService,
        private el: ElementRef,
        private renderer: Renderer2,
    ) {
        const hostElement = el.nativeElement;
        this.renderer.addClass(hostElement, "ion-page");
    }
    // Changes applied together
    public applyChanges() {
        this.emitForm.emit(this.formGroup ?? null);
        return;
    }
}
