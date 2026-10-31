import { LOCALE_ID, signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormsModule } from "@angular/forms";
import { By } from "@angular/platform-browser";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { InfiniteScrollCustomEvent, IonicModule } from "@ionic/angular";
import { FORMLY_CONFIG } from "@ngx-formly/core";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { BehaviorSubject } from "rxjs";
import { routes } from "src/app/app-routing.module";
import { Theme } from "src/app/edge/history/shared";
import { PlatFormService } from "src/app/platform.service";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { FlatWidgetButtonComponent } from "src/app/shared/components/flat/flat-widget-button/flat-widget-button";
import { User } from "src/app/shared/jsonrpc/shared";
import { AuthService } from "src/app/shared/service/auth/auth.service";
import { Pagination } from "src/app/shared/service/pagination";
import { RouteService } from "src/app/shared/service/route/route.service";
import { UserService } from "src/app/shared/service/user.service";
import { Edge, Service, Utils, Websocket } from "src/app/shared/shared";
import { registerTranslateExtension } from "src/app/shared/translate.extension";
import { Language, MyTranslateLoader } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { FilterComponent } from "../filter/filter.component";
import { OverViewComponent } from "./overview.component";

describe("OverviewComponent", () => {
    let component: OverViewComponent;
    let fixture: ComponentFixture<OverViewComponent>;
    const serviceSpyObject = jasmine.createSpyObj<Service>(
        "Service",
        ["getCurrentEdge", "getEdges", "getIsSmartphoneResolution"],
        {
            metadata: new BehaviorSubject({
                edges: {},
                user: new User("", "", "admin", "", true, { theme: Theme.LIGHT }),
            }),
            getIsSmartphoneResolution: () => false,
            getEdges(): Promise<Edge[]> {
                return Promise.resolve([]);
            },
        },
    );

    const userServiceSpyObj = jasmine.createSpyObj<UserService>("UserService", ["currentUser"], {
        currentUser: signal(new User("", "", "admin", "", true, { theme: Theme.LIGHT })),
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [
                TranslateModule.forRoot({
                    loader: { provide: TranslateLoader, useClass: MyTranslateLoader },
                    fallbackLang: Language.DEFAULT.key,
                }),
                IonicModule.forRoot(),
                RouterModule.forRoot(routes),
                FlatWidgetButtonComponent,
                FilterComponent,
                FormsModule,
            ],
            declarations: [OverViewComponent],
            providers: [
                {
                    provide: PlatFormService,
                    useValue: {
                        isSmartphone: () => false,
                    },
                },
                { provide: Service, useValue: serviceSpyObject },
                { provide: UserService, useValue: userServiceSpyObj },
                {
                    provide: FORMLY_CONFIG,
                    multi: true,
                    useFactory: registerTranslateExtension,
                    deps: [TranslateService],
                },
                { provide: LOCALE_ID, useValue: Language.DEFAULT.key },
                { provide: Websocket, useValue: {} },
                TranslateService,
                Pagination,
                Utils,
                Router,
                {
                    provide: ActivatedRoute,
                    useValue: {},
                },
                AuthService,
                PlatFormService,
                RouteService,
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(OverViewComponent);
                component = fixture.componentInstance;
                fixture.detectChanges();
            });
    });

    it("+loggedInUserCanInstall & ibn-button exists - Global role ADMIN", async () => {
        const button = await getIbnButtonElement(component, fixture, "installer");
        expect(component.loggedInUserCanInstall()).toEqual(true);
        expect(button).toBeTruthy();
    });

    it("+loggedInUserCanInstall & ibn-button doesnt exist - Global role OWNER", async () => {
        const button = await getIbnButtonElement(component, fixture, "owner");
        expect(component.loggedInUserCanInstall()).toEqual(false);
        expect(button).toBeNull();
    });

    it("keeps search controls available when no edges match", async () => {
        await fixture.whenStable();
        component.environment = { ...component.environment, backend: "OpenEMS Backend" };
        component.noEdges = true;
        fixture.changeDetectorRef.markForCheck();
        fixture.detectChanges();

        expect(fixture.debugElement.query(By.css("ion-searchbar"))).not.toBeNull();
        expect(fixture.debugElement.query(By.css("oe-filter"))).not.toBeNull();
    });

    async function getIbnButtonElement(
        component: OverViewComponent,
        fixture: ComponentFixture<OverViewComponent>,
        globalRole: "installer" | "owner",
    ) {
        const user: User = new User("", "test.user", globalRole, Language.DE.key, true, {});
        userServiceSpyObj.currentUser.set(user);
        serviceSpyObject.metadata.next({
            edges: { ["edge0"]: DummyConfig.dummyEdge({ role: Role.INSTALLER }) },
            user: new User("", "test.user", globalRole, Language.DE.key, true, {}),
        });

        component.ngOnInit();
        await fixture.whenStable();
        fixture.detectChanges();
        const { debugElement } = fixture;
        return debugElement.query(By.css('[testId="ibn-button"]'));
    }
});

describe("OverviewComponent search requests", () => {
    let component: OverViewComponent;
    let service: jasmine.SpyObj<Service>;

    beforeEach(() => {
        service = jasmine.createSpyObj<Service>("Service", ["getEdges"]);
        TestBed.configureTestingModule({
            providers: [
                { provide: Service, useValue: service },
                { provide: Websocket, useValue: {} },
                { provide: TranslateService, useValue: {} },
                { provide: Pagination, useValue: {} },
                { provide: ActivatedRoute, useValue: {} },
                { provide: Router, useValue: jasmine.createSpyObj("Router", ["navigate"]) },
                { provide: UserService, useValue: { currentUser: signal(null) } },
            ],
        });
        component = TestBed.runInInjectionContext(
            () =>
                new OverViewComponent(
                    service,
                    TestBed.inject(Websocket),
                    TestBed.inject(TranslateService),
                    TestBed.inject(Pagination),
                    TestBed.inject(ActivatedRoute),
                    TestBed.inject(Router),
                    TestBed.inject(UserService),
                ),
        );
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    it("ignores the initial response when a filtered search finishes first", async () => {
        const initial = deferredEdges();
        const filtered = deferredEdges();
        const edges = [DummyConfig.dummyEdge({ role: Role.OWNER })];
        service.getEdges.and.returnValues(initial.promise, filtered.promise);

        component.ngOnInit();
        component["searchOnChange"](new Map([["status", ["active"]]]));
        filtered.resolve(edges);
        await settleRequests();
        initial.resolve([]);
        await settleRequests();

        expect(component.filteredEdges()).toEqual(edges);
        expect(component.noEdges).toBeFalse();
    });

    it("does not let an old response clear loading or stop pagination", async () => {
        const initial = deferredEdges();
        const filtered = deferredEdges();
        const edges = Array.from({ length: 20 }, () => DummyConfig.dummyEdge({ role: Role.OWNER }));
        service.getEdges.and.returnValues(initial.promise, filtered.promise);

        component.ngOnInit();
        component["searchOnChange"](new Map([["status", ["active"]]]));
        initial.resolve([]);
        await settleRequests();

        expect(component["loading"]()).toBeTrue();
        expect(component["limitReached"]).toBeFalse();
        expect(component.noEdges).toBeFalse();

        filtered.resolve(edges);
        await settleRequests();

        expect(component.filteredEdges()).toEqual(edges);
        expect(component["loading"]()).toBeFalse();
        expect(component["limitReached"]).toBeFalse();
    });

    it("updates the empty state after each accepted search", async () => {
        const edges = [DummyConfig.dummyEdge({ role: Role.OWNER })];
        service.getEdges.and.returnValues(Promise.resolve([]), Promise.resolve(edges));

        component.ngOnInit();
        await settleRequests();
        expect(component.noEdges).toBeTrue();

        component["searchOnChange"]();
        await settleRequests();
        expect(component.noEdges).toBeFalse();
        expect(component.filteredEdges()).toEqual(edges);
    });

    it("does not append a stale pagination response to a new search", async () => {
        const nextPage = deferredEdges();
        const edges = [DummyConfig.dummyEdge({ role: Role.OWNER })];
        const complete = jasmine.createSpy("complete");
        service.getEdges.and.returnValues(nextPage.promise, Promise.resolve(edges));

        jasmine.clock().install();
        component.doInfinite({ target: { complete } } as unknown as InfiniteScrollCustomEvent);
        jasmine.clock().tick(200);
        jasmine.clock().uninstall();
        component["searchOnChange"]();
        await settleRequests();
        nextPage.resolve([DummyConfig.dummyEdge({ role: Role.OWNER })]);
        await settleRequests();

        expect(component.filteredEdges()).toEqual(edges);
        expect(complete).toHaveBeenCalledTimes(1);
    });

    it("does not start a scheduled page after the search changes", async () => {
        const complete = jasmine.createSpy("complete");
        service.getEdges.and.returnValue(Promise.resolve([]));

        jasmine.clock().install();
        component.doInfinite({ target: { complete } } as unknown as InfiniteScrollCustomEvent);
        component["searchOnChange"]();
        jasmine.clock().tick(200);
        jasmine.clock().uninstall();
        await settleRequests();

        expect(service.getEdges).toHaveBeenCalledTimes(1);
        expect(complete).toHaveBeenCalledTimes(1);
    });

    function settleRequests(): Promise<void> {
        return new Promise((resolve) => setTimeout(resolve, 0));
    }

    function deferredEdges() {
        let resolve!: (edges: Edge[]) => void;
        const promise = new Promise<Edge[]>((resolvePromise) => {
            resolve = resolvePromise;
        });
        return { promise, resolve };
    }
});
