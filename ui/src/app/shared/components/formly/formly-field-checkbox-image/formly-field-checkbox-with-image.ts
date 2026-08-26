import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from "@angular/core";
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
        .resize-image {
            max-width: 20rem;
            height: auto;
        }
        .max-width-image {
            max-width: 100%;
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
        @media (max-width: 600px) {
            .resize-image {
                max-width: 10rem;
                height: auto;
            }
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

    constructor(private cdr: ChangeDetectorRef) {
        super();
    }

    protected get shouldShowError(): boolean {
        // Is there actually an error?
        if (!this.formControl.hasError("serialNumberRequired")) {
            return false;
        }

        // nested input
        if (this.serialNumberFormControl.touched || this.serialNumberFormControl.dirty) {
            return true;
        }

        // form submission state
        if (this.options?.parentForm?.submitted) {
            return true;
        }

        return false;
    }

    protected get borderColor(): { [key: string]: string } {
        // If the checkbox is unchecked, always use default color
        if (this.value !== true) {
            return { "border-color": "var(--ion-color-dark)" };
        }

        if (this.shouldShowError) {
            return { "border-color": "var(--ion-color-danger)" };
        }

        const hasSerialError = this.formControl.hasError("serialNumberRequired");
        const isDirty = this.serialNumberFormControl.dirty;
        const hasValue = !!this.serialNumberFormControl.value;
        const isValid = !hasSerialError && (this.serialNumberFormControl.touched || isDirty || hasValue);

        if (isValid) {
            return { "border-color": "var(--ion-color-success)" };
        }

        if (this.isSerialNumberFocused) {
            return { "border-color": "var(--ion-color-primary)" };
        }

        return { "border-color": "var(--ion-color-dark)" };
    }

    public ngOnInit() {
        this.value = this.formControl.value ?? this.field.defaultValue ?? false;

        // Listen to form control status changes (e.g., from parent form)
        this.formControl.statusChanges.pipe(takeUntil(this.destroy)).subscribe((status) => {
            if (status === "DISABLED" && this.value !== false) {
                this.value = false;
                this.formControl.setValue(this.value);
                this.cdr.markForCheck();
            }
        });

        // Keep the rendered checkbox in sync when the control's value is changed
        // programmatically (e.g. from another field's hook), not just via user click.
        this.formControl.valueChanges.pipe(takeUntil(this.destroy)).subscribe((value) => {
            if (this.value !== value) {
                this.value = value;
                this.cdr.markForCheck();
            }
        });

        // Initialize the nested serial number field if it's configured
        if (this.props && "serialNumberField" in this.props) {
            this.initializeSerialNumberField();
        }
    }

    public ngOnDestroy() {
        this.destroy.next();
        this.destroy.complete();
    }

    /** Needs to be updated manually, because @Angular Formly-Form doesnt do it on its own */
    protected updateFormControl(event: CustomEvent) {
        this.value = event.detail.checked;
        this.formControl.setValue(this.value);
        this.cdr.markForCheck();
    }

    /**
     * Returns the show/hide value based on the properties.
     *
     * @returns Boolean value representing "show" or "hide".
     */
    protected showContent() {
        return !this.props?.disabled && !this.value && Boolean(this.props?.["url"]);
    }

    private initializeSerialNumberField(): void {
        const snProps = this.props.serialNumberField;
        const snKey = snProps.key;

        if (snKey == null || snKey === "") {
            console.error("The 'serialNumberField' property must have a 'key' to bind to the model.", this.field);
            return;
        }

        this.serialNumberFormControl = new FormControl();

        this.serialNumberFormControl.setValue(this.model[snKey], {
            emitEvent: false,
        });
        this.serialNumberFormControl.valueChanges.pipe(takeUntil(this.destroy)).subscribe((value) => {
            this.model[snKey] = value;
            this.formControl.updateValueAndValidity();
            this.cdr.markForCheck();
        });

        this.formControl.valueChanges.pipe(takeUntil(this.destroy)).subscribe((isChecked) => {
            if (isChecked) {
                this.serialNumberFormControl.enable({ emitEvent: false });
            } else {
                this.serialNumberFormControl.disable({ emitEvent: false });
                this.serialNumberFormControl.reset(undefined, {
                    emitEvent: false,
                }); // Clear value when disabled
            }
            this.cdr.markForCheck();
        });

        if (this.formControl.value === true) {
            this.serialNumberFormControl.enable();
        } else {
            this.serialNumberFormControl.disable();
        }
    }
}
