import { CommonModule } from "@angular/common";
import { Component, ViewEncapsulation, ChangeDetectionStrategy } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { FieldWrapper } from "@ngx-formly/core";

type HintIcon = "Info" | "Warning" | "Error";

@Component({
    selector: "formly-checkbox-with-hint",
    standalone: true,
    imports: [CommonModule, IonicModule],
    encapsulation: ViewEncapsulation.None,
    styles: [
        `
            .formly-hint {
                --min-height: 30px;

                ion-icon {
                    margin-right: 8px;
                }

                ion-text {
                    font-size: 0.8rem;
                    line-height: 1.2;
                }

                &.info ion-icon {
                    color: #052b5c;
                }

                &.warning ion-icon {
                    color: var(--ion-color-warning);
                }

                &.error ion-icon {
                    color: var(--ion-color-danger);
                }
            }
        `,
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    template: `
        <ion-item lines="none" class="formly-hint" [ngClass]="(props.icon ?? 'Info').toLowerCase()">
            <ion-icon [name]="getIconName(props.icon)" slot="start"> </ion-icon>

            <ion-text [innerHTML]="props.hint"></ion-text>
        </ion-item>

        <ng-template #fieldComponent></ng-template>
    `,
})
export class FormlyCheckboxWithHintWrapper extends FieldWrapper {
    protected getIconName(icon?: HintIcon): string {
        switch (icon) {
            case "Warning":
                return "oe-warning";
            case "Error":
                return "oe-error";
            default:
                return "oe-info";
        }
    }
}
