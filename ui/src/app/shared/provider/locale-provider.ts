import { LOCALE_ID, NgModule } from "@angular/core";
import { Language } from "../type/language";

@NgModule({
    providers: [
        {
            provide: LOCALE_ID,
            useFactory: () => Language.getCurrentLanguage().key,
        },
    ],
})
export class LocaleProvider { }
