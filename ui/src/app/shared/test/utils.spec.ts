import { registerLocaleData } from "@angular/common";
import localeDe from '@angular/common/locales/de';
import localeDeExtra from '@angular/common/locales/extra/de';
import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { registerTranslateExtension } from "../translate.extension";
import { Language, MyTranslateLoader } from "../type/language";

export type TestContext = { translate: TranslateService };

export function sharedSetup(): TestContext {
    TestBed.configureTestingModule({
        imports: [
            TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: Language.DEFAULT.key })
        ],
        providers: [TranslateService, { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] }]
    }).compileComponents();
    registerLocaleData(localeDe, 'de', localeDeExtra);
    return { translate: TestBed.inject(TranslateService) };
};
