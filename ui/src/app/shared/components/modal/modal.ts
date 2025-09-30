// @ts-strict-ignore
import { Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../shared";
import { Role } from "../../type/role";
import { Icon } from "../../type/widget";
import { HelpButtonComponent } from "./help-button/help-button";

export enum Status {
    SUCCESS,
    ERROR,
    PENDING,
}

@Component({
    selector: "oe-modal",
    templateUrl: "./MODAL.HTML",
    styles: [`
        :host {
            height: 100%;
            font-size: 0.9em;
        }
    `],
    standalone: false,
})
export class ModalComponent {

    /** Title in Header */
    @Input({ required: true }) public title!: string | null;

    @Input() protected component: EDGE_CONFIG.COMPONENT | null = null;
    @Input() protected formGroup: FormGroup = new FormGroup({});
    @Input() protected toolbarButtons: { url: string, icon: Icon }[] | { url: string, icon: Icon } | {
        callback: () =>
            {}, icon: Icon
    } | null = null;
    @Input() protected helpKey: HelpButtonComponent["key"] | null = null;

    @Input() protected useDefaultPrefix: HelpButtonComponent["useDefaultPrefix"] = false;
    public readonly Role = Role;

    private edge: Edge | null = null;

    constructor(
        public modalController: ModalController,
        private websocket: Websocket,
        private service: Service,
        private translate: TranslateService,
    ) {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => THIS.EDGE = edge);
    }

    // Changes applied together
    public async applyChanges() {
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
}
