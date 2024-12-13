
import { registerLocaleData } from "@angular/common";
import localDE from "@angular/common/locales/de";
import localeDeExtra from "@angular/common/locales/extra/de";
import { LOCALE_ID } from "@angular/core";
import { TestBed, TestModuleMetadata } from "@angular/core/testing";
import { ActivatedRoute, RouterModule } from "@angular/router";
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

function setTranslateParams(): Promise<void> {
    return new Promise<void>((res) => {
        const translateService = TestBed.inject(TranslateService);
        translateService.addLangs(["de"]);
        translateService.use("de");
        registerLocaleData(localDE, "de", localeDeExtra);
        res();
    });
}

export namespace TestingUtils {

    /**
     * Sets up a basic testing environment setup
     *
     * @returns the injected translateService and service
     */
    export async function sharedSetup(): Promise<TestContext> {
        await TestBed.configureTestingModule(BASE_TEST_BED)
            .compileComponents()
            .then(() => setTranslateParams());

        return {
            translate: TestBed.inject(TranslateService),
            service: TestBed.inject(Service),
        };
    }

    // Main function that returns an object with service names and injected providers
    /**
     * Merges setups and injects additional services to the base setup - {@link sharedSetup}
     *
     * @param services the services to be injected
     * @returns the merged testing environment setup
     */
    export async function mergeSetup<T extends Record<string, any>>(
        services: { name: keyof T; provider: new (...args: any[]) => T[keyof T]; metadata: TestModuleMetadata }[],
    ): Promise<T> {
        // Merge all TestModuleMetadata from services
        const testModuleMetadata = services.reduce((arr, el) => {
            arr.imports.push(...(el.metadata.imports || []));
            arr.providers.push(...(el.metadata.providers || []));
            return arr;
        }, {
            imports: BASE_TEST_BED.imports ?? [],
            providers: BASE_TEST_BED.providers ?? [],
        });

        // Set up the TestBed
        await TestBed.configureTestingModule(testModuleMetadata)
            .compileComponents();
        await setTranslateParams();

        // Inject services and return them as an object with service names as keys
        const result = services.reduce((arr, el) => {
            arr[el.name] = TestBed.inject(el.provider);
            return arr;
        }, {} as T);

        return {
            translate: TestBed.inject(TranslateService),
            service: TestBed.inject(Service),
            ...result,
        };
    }

    export namespace TestModuleMetadata {

        /**
         * Sets the activatedRoute testmetadata
         *
         * @param componentId the component id
         * @returns the test module data, needed for setting up testing environment
         */
        export function setActivatedRoute(componentId: string): TestModuleMetadata {
            return {
                imports: [
                    RouterModule.forRoot(routes),
                ],
                providers: [
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            snapshot: {
                                params: { componentId: componentId },
                            },
                        },
                    },
                ],
            };
        }
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

    /**
     * Sets up the test environment for components with own route
     *
     * @param componentId the component id
     * @returns the test context with the activatedRoute services injected
     */
    export function setupWithActivatedRoute(componentId: string): Promise<(TestContext & { route: ActivatedRoute; })> {
        return TestingUtils.mergeSetup<TestContext & { route: ActivatedRoute }>([{ name: "route", provider: ActivatedRoute, metadata: TestingUtils.TestModuleMetadata.setActivatedRoute(componentId) }]);
    }
}
