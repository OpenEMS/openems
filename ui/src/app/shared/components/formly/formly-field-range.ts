import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-range-type",
    template: `
        @if (props.note) {
            <p class="ion-padding-top">{{ props.note }}</p>
        }
        <ion-range
            class="ion-padding-top"
            label-placement="stacked"
            [class.range-invalid]="showError"
            [label]="props.label"
            [value]="props.defaultValue"
            [min]="props.min"
            [max]="props.max"
            [step]="props.step || 1"
            [pin]="props.pin ?? props.attributes?.pin"
            [snaps]="props.snaps || false"
            [formControl]="formControl"
            [formlyAttributes]="field"
            [pinFormatter]="boundPinFormatter"
            (ionChange)="onChange($event)"
        >
            <ion-label slot="start">{{ props.min }}</ion-label>
            <ion-label slot="end">{{ props.max }}</ion-label>
        </ion-range>

        <ion-text class="range-current-value">
            {{ "GENERAL.CURRENT_VALUE" | translate }}: {{ getCurrentValueLabel() }}
        </ion-text>

        @if (props.info) {
            <ion-grid>
                <ion-row class="ion-justify-content-center">
                    <ion-col size="12" size-sm="12" size-md="10" size="8">
                        <ion-item>
                            <ion-icon slot="start" size="large" color="primary" name="oe-info"></ion-icon>
                            <ion-text text-nowrap>{{ props.info }}</ion-text>
                        </ion-item>
                    </ion-col>
                </ion-row>
            </ion-grid>
        }

        <!-- Validation errors -->
        @if (showError) {
            <ion-text color="danger">
                @if (formControl.errors?.required) {
                    <p>
                        {{ props.required ? props.label + " " + ("GENERAL.FORMLY.REQUIRED" | translate) : "" }}
                    </p>
                }
            </ion-text>
        }
    `,
    styles: [
        `
            ion-range.range-invalid::part(label) {
                color: var(--ion-color-danger);
            }

            .range-current-value {
                display: block;
                text-align: left;
                font-size: 0.8rem;
            }
        `,
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class FormlyRangeTypeComponent extends FieldType {
    protected boundPinFormatter = this.pinFormatter.bind(this);

    public onChange(event: any): void {
        if (this.props.change) {
            this.props.change(this.field);
        }
    }

    protected pinFormatter(value: number): string {
        const unit = this.props?.unit || "";
        return `${value}${unit}`;
    }

    protected getCurrentValueLabel(): string {
        const value = this.formControl.value;
        if (value === undefined || value === null || value === "" || Number.isNaN(Number(value))) {
            return this.pinFormatter(Number(this.props.defaultValue ?? this.props.min ?? 0));
        }
        return this.pinFormatter(Number(value));
    }
}
