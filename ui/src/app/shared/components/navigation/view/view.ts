// @ts-strict-ignore
import { Component, effect, Input, untracked } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../shared";
import { NavigationComponent } from "../NAVIGATION.COMPONENT";
import { NavigationService } from "../service/NAVIGATION.SERVICE";
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
    templateUrl: "./VIEW.HTML",
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

    @Input() protected component: EDGE_CONFIG.COMPONENT | null = null;
    @Input() protected formGroup: FormGroup = new FormGroup({});

    protected contentHeight: number | null = null;

    private edge: Edge | null = null;

    constructor(
        public modalController: ModalController,
        protected service: Service,
        protected navigationService: NavigationService,
        private websocket: Websocket,
        private translate: TranslateService,
    ) {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => THIS.EDGE = edge);

        effect(() => {
            const breakpoint = NAVIGATION_COMPONENT.BREAK_POINT();
            if (breakpoint > NavigationComponent.INITIAL_BREAKPOINT) {
                return;
            }
            THIS.CONTENT_HEIGHT = VIEW_UTILS.GET_VIEW_HEIGHT(untracked(() => THIS.NAVIGATION_SERVICE.POSITION()));
        });
    }

    // Changes applied together
    public applyChanges() {
        const updateComponentArray: { name: string, value: any }[] = [];
        THIS.SERVICE.START_SPINNER("spinner");
        for (const key in THIS.FORM_GROUP.CONTROLS) {
            const control = THIS.FORM_GROUP.CONTROLS[key];
            THIS.FORM_GROUP.CONTROLS[key];

            // Check if formControl-value didn't change
            if (CONTROL.PRISTINE) {
                continue;
            }

            UPDATE_COMPONENT_ARRAY.PUSH({
                name: key,
                value: THIS.FORM_GROUP.VALUE[key],
            });
        }

        if (THIS.EDGE) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, updateComponentArray)
                .then(() => {
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
                }).catch(reason => {
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                }).finally(() => THIS.SERVICE.STOP_SPINNER("spinner"));
        }
        THIS.FORM_GROUP.MARK_AS_PRISTINE();
    }

    protected onDomChange() {
        THIS.CONTENT_HEIGHT = VIEW_UTILS.GET_VIEW_HEIGHT(THIS.NAVIGATION_SERVICE.POSITION());
    }
}
