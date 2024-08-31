
import { registerLocaleData } from "@angular/common";
import localDE from "@angular/common/locales/de";
import localeDeExtra from "@angular/common/locales/extra/de";
import { LOCALE_ID } from "@angular/core";
import { TestBed, TestModuleMetadata } from "@angular/core/testing";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { routes } from "src/app/app-routing.module";
import { Service } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";

export type TestContext = { translate: TranslateService, service: Service };

export const BASE_TEST_BED: TestModuleMetadata = {
    imports: [
        TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: Language.DEFAULT.key, useDefaultLang: false }),
    ],
    providers: [
        TranslateService,
        { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
        { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
        Service,
    ],
};

export function setTranslateParams(): void {
    const translateService = TestBed.inject(TranslateService);
    translateService.addLangs(["de"]);
    translateService.use("de");
    registerLocaleData(localDE, "de", localeDeExtra);
}

export async function sharedSetup(): Promise<TestContext> {
    await TestBed.configureTestingModule(BASE_TEST_BED)
        .compileComponents()
        .then(() => setTranslateParams());

    return {
        translate: TestBed.inject(TranslateService),
        service: TestBed.inject(Service),
    };
}

export function removeFunctions(obj: any): any {
    if (typeof obj !== "object" || obj === null) {
        return obj;
    }

    const result: any = {};
    for (const key in obj) {
        if (typeof obj[key] !== "function") {
            result[key] = removeFunctions(obj[key]);
        }
    }
    return result;
}

export async function sharedSetupWithComponentIdRoute(componentId: string): Promise<TestContext & { route: ActivatedRoute }> {
    await TestBed.configureTestingModule({
        imports: [
            ...(BASE_TEST_BED.imports as any[]),
            RouterTestingModule.withRoutes(routes),
            RouterModule.forRoot([]),
        ],
        providers: [
            ...(BASE_TEST_BED.providers as any[]),
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        params: { componentId: componentId },
                    },
                },
            },
        ],
    })
        .compileComponents()
        .then(() => setTranslateParams());

    return {
        translate: TestBed.inject(TranslateService),
        service: TestBed.inject(Service),
        route: TestBed.inject(ActivatedRoute),
    };
}
