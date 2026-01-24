import { Component, Input, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";

@Component({
    selector: "select-with-image-modal",
    templateUrl: "./select-with-image-modal.component.html",
    styles: [`
        ion-label {
            white-space: normal;
        }

        .option-image-container {
            margin: 0.5rem 0;
            border-radius: 0.5rem;
            overflow: hidden;
            max-width: 12.5rem;
        }

        .option-image {
            width: 100%;
            height: auto;
        }

        .option-label {
            display: flex;
            align-items: center;
            gap: 0.5rem; /* controls spacing between text and link */
            flex-wrap: wrap; /* optional if text is long */
            white-space: normal;
            }

        .doc-link {
            font-size: 0.9em;
            color: var(--ion-color-primary);
            text-decoration: underline;
        }
    `],
    standalone: false,
})
export class FormlySelectOptionsWithImageModalComponent implements OnInit {
    @Input() public options: any[] = [];
    @Input() public value: any;
    @Input() public title!: string;

    protected selectedValue: any;

    constructor(private modalCtrl: ModalController) { }

    public ngOnInit() {
        this.selectedValue = this.value;
    }

    protected selectOption(value: any) {
        this.selectedValue = value;
    }

    protected cancel() {
        return this.modalCtrl.dismiss(null, "cancel");
    }

    protected confirm() {
        return this.modalCtrl.dismiss(this.selectedValue, "confirm");
    }
}
