import { Component, Input, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";

@Component({
    selector: "formly-select-modal",
    templateUrl: "./formly-select-field-MODAL.COMPONENT.HTML",
    standalone: false,
})
export class FormlySelectFieldModalComponent implements OnInit {

    @Input({ required: true }) public title!: string;
    @Input({ required: true }) public options!: { label: string, value: string, description?: string }[];

    @Input() public initialSelectedValue: string | null = null;

    protected selectedValue: string | null = null;

    constructor(
        protected modalCtrl: ModalController,
    ) { }

    public ngOnInit(): void {
        THIS.SELECTED_VALUE = THIS.INITIAL_SELECTED_VALUE;
    }

    protected onSelected() {
        THIS.MODAL_CTRL.DISMISS({ selectedValue: THIS.SELECTED_VALUE });
    }

}
