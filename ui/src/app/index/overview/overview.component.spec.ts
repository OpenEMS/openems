// @ts-strict-ignore
import { LOCALE_ID, signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { Theme } from "src/app/edge/history/shared";
import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { User } from "src/app/shared/jsonrpc/shared";
import { Pagination } from "src/app/shared/service/pagination";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/TRANSLATE.EXTENSION";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { OverViewComponent } from "./OVERVIEW.COMPONENT";

describe("OverviewComponent", () => {
    let component: OverViewComponent;
    let fixture: ComponentFixture<OverViewComponent>;
    const serviceSpyObject = JASMINE.CREATE_SPY_OBJ<Service>("Service", ["getCurrentEdge", "getEdges"], {
        metadata: new BehaviorSubject({
            edges: null,
            user: null,
        }),
        getEdges(): Promise<Edge[]> {
            return PROMISE.RESOLVE([]);
        },
    });

    const userServiceSpyObj = JASMINE.CREATE_SPY_OBJ<UserService>("UserService", ["currentUser"], {
        currentUser: signal(new User("", "", "admin", "", true, { theme: THEME.LIGHT })),
    });

    beforeEach(async () => {
        await TEST_BED.CONFIGURE_TESTING_MODULE({
            imports: [TRANSLATE_MODULE.FOR_ROOT({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: LANGUAGE.DEFAULT.KEY, useDefaultLang: false }),
            ],
            declarations: [OverViewComponent],
            providers: [
                { provide: Service, useValue: serviceSpyObject },
                { provide: UserService, useValue: userServiceSpyObj },
                Websocket,
                TranslateService,
                { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
                { provide: LOCALE_ID, useValue: LANGUAGE.DEFAULT.KEY },
                Pagination,
                Utils,
                IonicModule,
                Router,
            ],
        }).compileComponents().then(() => {
            fixture = TEST_BED.CREATE_COMPONENT(OverViewComponent);
            component = FIXTURE.COMPONENT_INSTANCE;
            FIXTURE.DETECT_CHANGES();
        });
    });

    it("+loggedInUserCanInstall & ibn-button exists - Global role ADMIN", async () => {
        const button = await getIbnButtonElement(component, fixture, "installer");
        expect(COMPONENT.LOGGED_IN_USER_CAN_INSTALL).toEqual(true);
        expect(button).toBeTruthy();
    });

    it("+loggedInUserCanInstall & ibn-button doesnt exist - Global role OWNER", async () => {
        const button = await getIbnButtonElement(component, fixture, "owner");
        expect(COMPONENT.LOGGED_IN_USER_CAN_INSTALL).toEqual(false);
        expect(button).toBeNull();
    });

    async function getIbnButtonElement(component: OverViewComponent, fixture: ComponentFixture<OverViewComponent>, globalRole: "installer" | "owner") {
        SERVICE_SPY_OBJECT.METADATA.NEXT({
            edges: { ["edge0"]: DUMMY_CONFIG.DUMMY_EDGE({ role: ROLE.INSTALLER }) },
            user: {
                globalRole: globalRole, hasMultipleEdges: true, id: "", language: LANGUAGE.DE.KEY, name: "TEST.USER", settings: {}, getThemeFromSettings() {
                    return THEME.LIGHT;
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

        COMPONENT.ION_VIEW_WILL_ENTER();
        await FIXTURE.WHEN_STABLE();
        FIXTURE.DETECT_CHANGES();
        const { debugElement } = fixture;
        return DEBUG_ELEMENT.QUERY(BY.CSS("[testId=\"ibn-button\"]"));
    }
});
