import { LOCALE_ID, NgModule } from "@angular/core";
import { Language } from "../type/language";

@NgModule({
    providers: [
        {
            provide: LOCALE_ID,
            useFactory: () => (
                Language.getByKey(localStorage.LANGUAGE) ?? Language.getByBrowserLang(navigator.language) ?? Language.DEFAULT).key,
        },
    ],
})
export class LocaleProvider { }
