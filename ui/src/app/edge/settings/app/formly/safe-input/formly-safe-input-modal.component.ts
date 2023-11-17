import { Component, Input, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { FormlyFieldConfig } from "@ngx-formly/core";

@Component({
    selector: 'formly-safe-input-modal',
    templateUrl: './formly-safe-input-modal.component.html'
})
export class FormlySafeInputModalComponent implements OnInit {

    @Input()
    protected title: string;
    @Input()
    protected fields: FormlyFieldConfig[] = null;
    @Input()
    protected model: {};

    protected form: FormGroup = new FormGroup({});
    protected myModel: {};

    constructor(
        protected modalCtrl: ModalController
    ) { }

    ngOnInit(): void {
        this.myModel = Object.assign({}, this.model);
    }

    protected onSelected() {
        this.modalCtrl.dismiss(this.myModel);
    }

}
