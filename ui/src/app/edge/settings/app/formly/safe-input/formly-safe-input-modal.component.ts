// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { FormlyFieldConfig } from "@ngx-formly/core";

@Component({
    selector: "formly-safe-input-modal",
    templateUrl: "./formly-safe-input-MODAL.COMPONENT.HTML",
    standalone: false,
})
export class FormlySafeInputModalComponent implements OnInit {

    @Input({ required: true })
    protected title!: string;
    @Input()
    protected fields: FormlyFieldConfig[] | null = null;
    @Input({ required: true })
    protected model!: {};

    protected form: FormGroup = new FormGroup({});
    protected myModel: {};

    constructor(
        protected modalCtrl: ModalController,
    ) { }

    ngOnInit(): void {
        THIS.MY_MODEL = OBJECT.ASSIGN({}, THIS.MODEL);
    }

    protected onSelected() {
        THIS.MODAL_CTRL.DISMISS(THIS.MY_MODEL);
    }

}
