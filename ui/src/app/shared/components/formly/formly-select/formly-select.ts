import { Component, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewEncapsulation } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { FieldType } from "@ngx-formly/core";
import { Subscription } from "rxjs";
import { FormlySelectOptionsWithImageModalComponent } from "./formly-select-with-image-modal/select-with-image-modal.component";

@Component({
    selector: "formly-custom-select",
    encapsulation: ViewEncapsulation.None,
    templateUrl: "/formly-select.html",
    standalone: false,
    styles: [`
        :host {
            width: 100%;
        }

        ion-item.custom-select-item::part(inner) {
            padding-right: 0 !important;
        }

        .ion-modal-fullscreen {
            --width: 100%;
            --max-width: 100%;
        }

        ion-select{
            color: inherit !important;
        }

        ion-select::part(label) {
            white-space:  pre-wrap !important;
            font-size: initial !important;
            flex: 1;
            min-width: 30%;
            max-width: 50%;
            margin-inline-end: 0 !important;
        }

        .custom-ion-alert {
                color: var(--ion-color-text) !important;
        }

        .custom-select-item {
            --inner-padding-end: 0;
            display: flex;
            align-items: center;

            ion-label {
                margin: 0;
                flex: 0 0 auto;
                min-width: 30%;
                max-width: 50%;
                white-space: pre-wrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }

            .select-value-container {
                display: flex;
                align-items: center;
                justify-content: flex-end;
                gap: 0.25rem;
                min-width: 0;
            }

            ion-text {
                flex: 1;
                font-size: 0.875rem;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                color: var(--ion-text-color);
            }

            ion-icon {
                flex-shrink: 0;
            }
        }
    `],
})
export class FormlySelectComponent extends FieldType implements OnInit, OnChanges, OnDestroy {

    protected hasImageOptions = false;
    protected selectOptions: any[] = [];
    protected selectedLabel: string | null = null;
    private subscription: Subscription = new Subscription();

    constructor(private modalCtrl: ModalController) {
        super();
    }

    public ngOnInit(): void {
        this.initializeOptions();
        this.getSelectedLabel();

        this.subscription.add(
            this.formControl.valueChanges.subscribe(() => this.getSelectedLabel())
        );
    }

    public ngOnChanges(changes: SimpleChanges) {
        // If props or its options change, recompute
        if (changes["props"]?.currentValue?.options) {
            this.initializeOptions();
            this.getSelectedLabel();
        }
    }

    public ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    async openOptionsModal() {
        const modal = await this.modalCtrl.create({
            component: FormlySelectOptionsWithImageModalComponent,
            cssClass: "ion-modal-fullscreen",
            componentProps: {
                options: this.selectOptions,
                value: this.formControl.value,
                title: this.props.label + (this.props.required ? "*" : ""),
            },
        });

        await modal.present();

        const { data, role } = await modal.onWillDismiss();

        if (role === "confirm" && data !== null) {
            // Update the form control value when the user confirms a selection
            this.formControl.setValue(data);
            this.formControl.markAsDirty();
        }
    }

    private getSelectedLabel() {
        const val = this.formControl.value;
        const selected = this.selectOptions.find(o => o.value === val);
        this.selectedLabel = selected?.label ?? null;
    }

    private initializeOptions() {
        this.selectOptions = (this.props.options as any[] ?? []);
        this.hasImageOptions = this.selectOptions.some(o => !!o.imageUrl);
    }
}
