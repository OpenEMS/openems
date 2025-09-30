import { registerLocaleData } from "@angular/common";
import localDE from "@angular/common/locales/de";
import localeDeExtra from "@angular/common/locales/extra/de";
import { LOCALE_ID } from "@angular/core";
import { TestBed, TestModuleMetadata } from "@angular/core/testing";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { routes } from "src/app/app-ROUTING.MODULE";
import { RouteService } from "src/app/shared/service/ROUTE.SERVICE";
import { Service } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/TRANSLATE.EXTENSION";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";

export type TestContext = { translate: TranslateService, service: Service };
export const BASE_TEST_BED: TestModuleMetadata = {
    imports: [
        TRANSLATE_MODULE.FOR_ROOT({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: LANGUAGE.DEFAULT.KEY, useDefaultLang: false }),
    ],
    providers: [
        TranslateService,
        { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
        { provide: LOCALE_ID, useValue: LANGUAGE.DEFAULT.KEY },
        Service,
        RouteService,
    ],
};

function setTranslateParams(): Promise<void> {
    return new Promise<void>((res) => {
        const translateService = TEST_BED.INJECT(TranslateService);
        TRANSLATE_SERVICE.ADD_LANGS(["de"]);
        TRANSLATE_SERVICE.USE("de");
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
        await TEST_BED.CONFIGURE_TESTING_MODULE(BASE_TEST_BED)
            .compileComponents()
            .then(() => setTranslateParams());

        return {
            translate: TEST_BED.INJECT(TranslateService),
            service: TEST_BED.INJECT(Service),
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
        const testModuleMetadata = SERVICES.REDUCE((arr, el) => {
            ARR.IMPORTS.PUSH(...(EL.METADATA.IMPORTS || []));
            ARR.PROVIDERS.PUSH(...(EL.METADATA.PROVIDERS || []));
            return arr;
        }, {
            imports: BASE_TEST_BED.imports ?? [],
            providers: BASE_TEST_BED.providers ?? [],
        });

        // Set up the TestBed
        await TEST_BED.CONFIGURE_TESTING_MODULE(testModuleMetadata)
            .compileComponents();
        await setTranslateParams();

        // Inject services and return them as an object with service names as keys
        const result = SERVICES.REDUCE((arr, el) => {
            arr[EL.NAME] = TEST_BED.INJECT(EL.PROVIDER);
            return arr;
        }, {} as T);

        return {
            translate: TEST_BED.INJECT(TranslateService),
            service: TEST_BED.INJECT(Service),
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
                    ROUTER_MODULE.FOR_ROOT(routes),
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
        return TESTING_UTILS.MERGE_SETUP<TestContext & { route: ActivatedRoute }>([{ name: "route", provider: ActivatedRoute, metadata: TESTING_UTILS.TEST_MODULE_METADATA.SET_ACTIVATED_ROUTE(componentId) }]);
    }
}
