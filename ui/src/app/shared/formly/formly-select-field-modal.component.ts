// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";

@Component({
    selector: 'formly-select-modal',
    templateUrl: './formly-select-field-modal.component.html',
})
export class FormlySelectFieldModalComponent implements OnInit {

    @Input() public title: string;
    @Input() public options: { label: string, value: string, description?: string }[];

    @Input() public initialSelectedValue: string | null = null;

    protected selectedValue: string | null = null;

    constructor(
        protected modalCtrl: ModalController,
    ) { }

    public ngOnInit(): void {
        this.selectedValue = this.initialSelectedValue;
    }

    protected onSelected() {
        this.modalCtrl.dismiss({ selectedValue: this.selectedValue });
    }

}
