// @ts-strict-ignore
import { Component, Input, OnInit, inject } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { FormlyFieldConfig } from "@ngx-formly/core";

@Component({
    selector: "formly-safe-input-modal",
    templateUrl: "./formly-safe-input-modal.component.html",
    standalone: false,
})
export class FormlySafeInputModalComponent implements OnInit {
    protected modalCtrl = inject(ModalController);


    @Input({ required: true })
    protected title!: string;
    @Input()
    protected fields: FormlyFieldConfig[] | null = null;
    @Input({ required: true })
    protected model!: {};

    protected form: FormGroup = new FormGroup({});
    protected myModel: {};

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    ngOnInit(): void {
        this.myModel = Object.assign({}, this.model);
    }

    protected onSelected() {
        this.modalCtrl.dismiss(this.myModel);
    }

}
