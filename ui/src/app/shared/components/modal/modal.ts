// @ts-strict-ignore
import { Component, Input, inject } from "@angular/core";
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
    templateUrl: "./modal.html",
    styles: [`
        :host {
            height: 100%;
            font-size: 0.9em;
        }
    `],
    standalone: false,
})
export class ModalComponent {
    modalController = inject(ModalController);
    private websocket = inject(Websocket);
    private service = inject(Service);
    private translate = inject(TranslateService);


    /** Title in Header */
    @Input({ required: true }) public title!: string | null;

    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected formGroup: FormGroup = new FormGroup({});
    @Input() protected toolbarButtons: { url: string, icon: Icon }[] | { url: string, icon: Icon } | {
        callback: () =>
            {}, icon: Icon
    } | null = null;
    @Input() protected helpKey: HelpButtonComponent["key"] | null = null;

    @Input() protected useDefaultPrefix: HelpButtonComponent["useDefaultPrefix"] = false;
    public readonly Role = Role;

    private edge: Edge | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        this.service.getCurrentEdge().then(edge => this.edge = edge);
    }

    // Changes applied together
    public async applyChanges() {
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
