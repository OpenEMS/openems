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
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { User } from "src/app/shared/jsonrpc/shared";
import { Pagination } from "src/app/shared/service/pagination";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { OverViewComponent } from "./overview.component";

describe("OverviewComponent", () => {
    let component: OverViewComponent;
    let fixture: ComponentFixture<OverViewComponent>;
    const serviceSpyObject = jasmine.createSpyObj<Service>("Service", ["getCurrentEdge", "getEdges"], {
        metadata: new BehaviorSubject({
            edges: null,
            user: null,
        }),
        getEdges(): Promise<Edge[]> {
            return Promise.resolve([]);
        },
    });

    const userServiceSpyObj = jasmine.createSpyObj<UserService>("UserService", ["currentUser"], {
        currentUser: signal(new User("", "", "admin", "", true, { theme: Theme.LIGHT })),
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot({ loader: { provide: TranslateLoader, useClass: MyTranslateLoader }, defaultLanguage: Language.DEFAULT.key, useDefaultLang: false }),
            ],
            declarations: [OverViewComponent],
            providers: [
                { provide: Service, useValue: serviceSpyObject },
                { provide: UserService, useValue: userServiceSpyObj },
                Websocket,
                TranslateService,
                { provide: FORMLY_CONFIG, multi: true, useFactory: registerTranslateExtension, deps: [TranslateService] },
                { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
                Pagination,
                Utils,
                IonicModule,
                Router,
            ],
        }).compileComponents().then(() => {
            fixture = TestBed.createComponent(OverViewComponent);
            component = fixture.componentInstance;
            fixture.detectChanges();
        });
    });

    it("+loggedInUserCanInstall & ibn-button exists - Global role ADMIN", async () => {
        const button = await getIbnButtonElement(component, fixture, "installer");
        expect(component.loggedInUserCanInstall).toEqual(true);
        expect(button).toBeTruthy();
    });

    it("+loggedInUserCanInstall & ibn-button doesnt exist - Global role OWNER", async () => {
        const button = await getIbnButtonElement(component, fixture, "owner");
        expect(component.loggedInUserCanInstall).toEqual(false);
        expect(button).toBeNull();
    });

    async function getIbnButtonElement(component: OverViewComponent, fixture: ComponentFixture<OverViewComponent>, globalRole: "installer" | "owner") {
        serviceSpyObject.metadata.next({
            edges: { ["edge0"]: DummyConfig.dummyEdge({ role: Role.INSTALLER }) },
            user: {
                globalRole: globalRole, hasMultipleEdges: true, id: "", language: Language.DE.key, name: "test.user", settings: {}, getThemeFromSettings() {
                    return Theme.LIGHT;
                },
                isAtLeast(role) {
                    return true;
                },
            },
        });

        component.ionViewWillEnter();
        await fixture.whenStable();
        fixture.detectChanges();
        const { debugElement } = fixture;
        return debugElement.query(By.css("[testId=\"ibn-button\"]"));
    }
});
