import { Component, Input, OnInit, inject } from "@angular/core";
import { ModalController } from "@ionic/angular";

@Component({
    selector: "formly-select-modal",
    templateUrl: "./formly-select-field-modal.component.html",
    standalone: false,
})
export class FormlySelectFieldModalComponent implements OnInit {
    protected modalCtrl = inject(ModalController);


    @Input({ required: true }) public title!: string;
    @Input({ required: true }) public options!: { label: string, value: string, description?: string }[];

    @Input() public initialSelectedValue: string | null = null;

    protected selectedValue: string | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    public ngOnInit(): void {
        this.selectedValue = this.initialSelectedValue;
    }

    protected onSelected() {
        this.modalCtrl.dismiss({ selectedValue: this.selectedValue });
    }

}
