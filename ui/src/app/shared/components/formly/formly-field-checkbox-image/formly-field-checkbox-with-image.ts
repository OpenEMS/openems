import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { FieldWrapper } from "@ngx-formly/core";
import { Subject, takeUntil } from "rxjs";

@Component({
    selector: "formly-field-checkbox-with-image",
    templateUrl: "./formly-field-checkbox-with-image.html",
    changeDetection: ChangeDetectionStrategy.OnPush,
    styles: `
        .center-image {
            display: block;
            margin-left: auto;
            margin-right: auto;
            max-width: 10rem;
            height: auto;
        }
        .input-box {
            border: 0.1em solid var(--ion-color-dark);
            border-radius: 0.125rem;
            width: 100%;
            padding-left: 0.5rem;
            --min-height: auto;
            --border-color: var(--ion-color-medium);
            transition: border-color 0.3s;
        }
        .disabled-sub-field {
            pointer-events: none;
            opacity: 0.5;
        }
        .help-icon {
            vertical-align: middle;
            font-size: 1.1em;
            margin-left: 0.25rem;
            color: var(--ion-color-medium-shade);
            cursor: help;
        }
    `,
    standalone: false,
})
export class FormlyFieldCheckboxWithImageComponent extends FieldWrapper implements OnInit, OnDestroy {

    protected value: any;

    // Properties for the nested serial number field
    protected serialNumberFormControl: FormControl = new FormControl();
    protected isSerialNumberFocused: boolean = false;

    //  Subject for subscription cleanup
    private destroy = new Subject<void>();

    protected get borderColor(): { [key: string]: string } {
        let borderColor = "var(--ion-color-dark)";

        if (this.value === true) {

            const validSerialNumber: string | null = this.model[this.props?.serialNumberField?.key] ?? null;
            if (validSerialNumber != null && validSerialNumber !== "") {
                borderColor = "var(--highlight-color-valid)";
            } else if (this.serialNumberFormControl.touched) {
                borderColor = "var(--highlight-color-invalid)";
            } else if (this.isSerialNumberFocused) {
                borderColor = "var(--highlight-color-focused)";
            }
        }
        return {
            "border-color": borderColor,
        };
    }

    public ngOnInit() {
        this.value = this.formControl.value ?? this.field.defaultValue;

        // Listen to form control status changes (e.g., from parent form)
        this.formControl.statusChanges.pipe(takeUntil(this.destroy)).subscribe(status => {
            if (status === "DISABLED" && this.value !== false) {
                this.value = false;
                this.formControl.setValue(this.value);
            }
        });

        // Initialize the nested serial number field if it's configured
        if ("serialNumberField" in this.props) {
            this.initializeSerialNumberField();
        }
    }

    public ngOnDestroy() {
        this.destroy.next();
        this.destroy.complete();
    }

    /**
     * Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own
     */
    protected updateFormControl(event: CustomEvent) {
        this.value = event.detail.checked;
        this.formControl.setValue(this.value);
    }

    /**
     * Returns the show/hide value based on the properties.
     *
     * @returns boolean value representing "show" or "hide".
     */
    protected showContent() {
        return (!this.field.props?.disabled && !this.value) && this.field.props?.url !== undefined;
    }

    private initializeSerialNumberField(): void {
        const snProps = this.props.serialNumberField;
        const snKey = snProps.key;

        if (snKey == null || snKey === "") {
            console.error("The 'serialNumberField' property must have a 'key' to bind to the model.", this.field);
            return;
        }

        this.serialNumberFormControl = new FormControl();

        this.serialNumberFormControl.setValue(this.model[snKey], { emitEvent: false });
        this.serialNumberFormControl.valueChanges.pipe(takeUntil(this.destroy)).subscribe(value => {
            this.model[snKey] = value;
            this.formControl.updateValueAndValidity();
        });

        this.formControl.valueChanges.pipe(takeUntil(this.destroy)).subscribe(isChecked => {
            if (isChecked) {
                this.serialNumberFormControl.enable({ emitEvent: false });
            } else {
                this.serialNumberFormControl.disable({ emitEvent: false });
                this.serialNumberFormControl.reset(undefined, { emitEvent: false }); // Clear value when disabled
            }
        });

        if (this.formControl.value === true) {
            this.serialNumberFormControl.enable();
        } else {
            this.serialNumberFormControl.disable();
        }
    }

}
