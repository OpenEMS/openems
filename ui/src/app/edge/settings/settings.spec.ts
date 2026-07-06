// @ts-strict-ignore
import { registerLocaleData } from "@angular/common";
import localDE from "@angular/common/locales/de";
import localeDeExtra from "@angular/common/locales/extra/de";
import { LOCALE_ID, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService, } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { User } from "src/app/shared/jsonrpc/shared";
import { UserService } from "src/app/shared/service/user.service";
import { Service, Utils } from "src/app/shared/shared";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { Theme } from "../history/shared";
import { registerTranslateExtension } from "./app/app.module";
import { SettingsComponent } from "./settings.component";

describe("Edge", () => {
    const serviceSypObject = jasmine.createSpyObj<Service>(
        "Service",
        ["getCurrentEdge"],
        {
            metadata: new BehaviorSubject({
                edges: null,
                user: new User(
                    "",
                    "test.user",
                    "admin",
                    Language.DE.key,
                    true,
                    {},
                ),
            }),
        },
    );

    let settingsComponent: SettingsComponent;
    const userServiceSpyObj = jasmine.createSpyObj<UserService>(
        "UserService",
        ["currentUser"],
        {
            currentUser: signal(
                new User("", "", "admin", "", true, { theme: Theme.LIGHT }),
            ),
        },
    );
    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot({
                    loader: {
                        provide: TranslateLoader,
                        useClass: MyTranslateLoader,
                    },
                    fallbackLang: Language.DEFAULT.key,
                }),
            ],
            providers: [
                { provide: UserService, useValue: userServiceSpyObj },
                TranslateService,
                {
                    provide: FORMLY_CONFIG,
                    multi: true,
                    useFactory: registerTranslateExtension,
                    deps: [TranslateService],
                },
                { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
                { provide: Service, useValue: serviceSypObject },
                Utils,
            ],
        })
            .compileComponents()
            .then(() => {
                const translateService = TestBed.inject(TranslateService);
                translateService.addLangs(["de"]);
                translateService.use("de");
                registerLocaleData(localDE, "de", localeDeExtra);
                settingsComponent = new SettingsComponent(
                    Utils,
                    serviceSypObject,
                    translateService,
                    userServiceSpyObj,
                );
            });
    });

    it("+ngOnInit - Role.ADMIN", async () => {
        const result = await expectNgOnInit(
            serviceSypObject,
            Role.ADMIN,
            settingsComponent,
        );
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: true,
            isAtLeastAdmin: true,
        });
    });
    it("+ngOnInit - Role.INSTALLER", async () => {
        const result = await expectNgOnInit(
            serviceSypObject,
            Role.INSTALLER,
            settingsComponent,
        );
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: true,
            isAtLeastAdmin: false,
        });
    });
    it("+ngOnInit - Role.OWNER", async () => {
        const result = await expectNgOnInit(
            serviceSypObject,
            Role.OWNER,
            settingsComponent,
        );
        expect(result).toEqual({
            isAtLeastOwner: true,
            isAtLeastInstaller: false,
            isAtLeastAdmin: false,
        });
    });
});

export async function expectNgOnInit(
    serviceSypObject: jasmine.SpyObj<Service>,
    edgeRole: Role,
    settingsComponent: SettingsComponent,
): Promise<{
    isAtLeastOwner: boolean;
    isAtLeastInstaller: boolean;
    isAtLeastAdmin: boolean;
}> {
    const edge = DummyConfig.dummyEdge({ role: edgeRole });
    serviceSypObject.getCurrentEdge.and.resolveTo(edge);
    serviceSypObject.metadata.next({
        edges: { [edge.id]: edge },
        user: new User("", "test.user", "admin", Language.DE.key, true, {}),
    });
    await settingsComponent.ngOnInit();
    return {
        isAtLeastOwner: settingsComponent.isAtLeastOwner(),
        isAtLeastInstaller: settingsComponent.isAtLeastInstaller,
        isAtLeastAdmin: settingsComponent.isAtLeastAdmin,
    };
}
