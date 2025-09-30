// @ts-strict-ignore
import { registerLocaleData } from "@angular/common";
import localDE from "@angular/common/locales/de";
import localeDeExtra from "@angular/common/locales/extra/de";
import { LOCALE_ID } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { Service, Utils } from "src/app/shared/shared";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { Theme } from "../history/shared";
import { registerTranslateExtension } from "./app/APP.MODULE";
import { SettingsComponent } from "./SETTINGS.COMPONENT";

describe("Edge", () => {
    const serviceSypObject = JASMINE.CREATE_SPY_OBJ<Service>("Service", ["getCurrentEdge"], {
        metadata: new BehaviorSubject({
            edges: null,
            user: {
                globalRole: "admin", hasMultipleEdges: true, id: "", language: LANGUAGE.DE.KEY, name: "TEST.USER", settings: {}, getThemeFromSettings() {
                    return null;
                },
                isAtLeast(role) {
                    return true;
                },
                getNavigationTree(navigation, translate) {
                    return null;
                },
                getUseNewUIFromSettings: function (): boolean {
                    throw new Error("Function not implemented.");
                },
            },
        }),
    });

    let settingsComponent: SettingsComponent;
    beforeEach(async () => {
        await TEST_BED.CONFIGURE_TESTING_MODULE({
            imports: [
                TRANSLATE_MODULE.FOR_ROOT({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: LANGUAGE.DEFAULT.KEY, useDefaultLang: false }),
            ],
            providers: [
                TranslateService,
                { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
                { provide: LOCALE_ID, useValue: LANGUAGE.DEFAULT.KEY },
                { provide: Service, useValue: serviceSypObject },
                Utils,
            ],
        }).compileComponents().then(() => {
            const translateService = TEST_BED.INJECT(TranslateService);
            TRANSLATE_SERVICE.ADD_LANGS(["de"]);
            TRANSLATE_SERVICE.USE("de");
            registerLocaleData(localDE, "de", localeDeExtra);
            settingsComponent = new SettingsComponent(Utils, serviceSypObject, translateService);
        });

    });

    it("+ngOnInit - ROLE.ADMIN", async () => {
        const result = await expectNgOnInit(serviceSypObject, ROLE.ADMIN, settingsComponent);
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: true,
            isAtLeastAdmin: true,
        });
    });
    it("+ngOnInit - ROLE.INSTALLER", async () => {
        const result = await expectNgOnInit(serviceSypObject, ROLE.INSTALLER, settingsComponent);
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: true,
            isAtLeastAdmin: false,
        });
    });
    it("+ngOnInit - ROLE.OWNER", async () => {
        const result = await expectNgOnInit(serviceSypObject, ROLE.OWNER, settingsComponent);
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: false,
            isAtLeastAdmin: false,
        });
    });
});

export async function expectNgOnInit(serviceSypObject: JASMINE.SPY_OBJ<Service>, edgeRole: Role, settingsComponent: SettingsComponent): Promise<{ isAtLeastOwner: boolean; isAtLeastInstaller: boolean; isAtLeastAdmin: boolean; }> {
    const edge = DUMMY_CONFIG.DUMMY_EDGE({ role: edgeRole });
    SERVICE_SYP_OBJECT.GET_CURRENT_EDGE.AND.RESOLVE_TO(edge);
    SERVICE_SYP_OBJECT.METADATA.NEXT({
        edges: { [EDGE.ID]: edge },
        user: {
            globalRole: "admin", hasMultipleEdges: true, id: "", language: LANGUAGE.DE.KEY, name: "TEST.USER", settings: {},
            getThemeFromSettings: function (): Theme | null {
                throw new Error("Function not implemented.");
            },
            isAtLeast(role) {
                return true;
            },
            getNavigationTree(navigation, translate) {
                return null;
            },
            getUseNewUIFromSettings: function (): boolean {
                throw new Error("Function not implemented.");
            },
        },
    });
    await SETTINGS_COMPONENT.NG_ON_INIT();
    return {
        isAtLeastOwner: SETTINGS_COMPONENT.IS_AT_LEAST_OWNER,
        isAtLeastInstaller: SETTINGS_COMPONENT.IS_AT_LEAST_INSTALLER,
        isAtLeastAdmin: SETTINGS_COMPONENT.IS_AT_LEAST_ADMIN,
    };
}
