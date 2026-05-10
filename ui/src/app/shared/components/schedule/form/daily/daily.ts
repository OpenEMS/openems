import { Component, model, ModelSignal } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Language } from "src/app/shared/type/language";
import de from "../../i18n/de.json";
import en from "../../i18n/en.json";

@Component({
    selector: "oe-schedule-task-form-daily",
    templateUrl: "./daily.html",
    imports: [
        CommonUiModule,
        NgxSpinnerModule,
        ReactiveFormsModule,
    ],
    styles: [
        `
            .datetime-button {
                &::part(native) {
                   background-color: var(--ion-color-toolbar-primary);
                   }

                &::part(content) {
                    padding: 0 !important;
                }
            }

            .picker-opts {
                --background: none;
            }

        `,
    ],
})
export class TaskFormTimeComponent {

    public startTime: ModelSignal<string | null> = model<string | null>(null);
    public endTime: ModelSignal<string | null> = model<string | null>(null);
    protected readonly spinnerId: string = uuidv4();

    constructor(private translate: TranslateService) {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    protected setTime(event: CustomEvent, signal: ModelSignal<string | null>) {
        signal.set(event.detail.value ?? this.convertTo15Min(new Date()));
    }

    private convertTo15Min(date: Date): ReturnType<Date["toISOString"]> {
        const minutes = date.getMinutes();
        const snapped = Math.round(minutes / 15) * 15;
        const d = new Date(date);
        d.setMinutes(snapped, 0, 0);
        return d.toISOString();
    }
}
