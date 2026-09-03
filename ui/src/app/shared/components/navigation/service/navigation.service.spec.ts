import { Component, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { Route, Routes, provideRouter } from "@angular/router";
import { RouterTestingHarness } from "@angular/router/testing";
import { AngularDelegate, ModalController } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { routes } from "src/app/app-routing.module";
import { settingsRoutes } from "src/app/edge/settings/settings-routing.module";
import { PlatFormService } from "src/app/platform.service";
import { User } from "src/app/shared/jsonrpc/shared";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { Pagination } from "src/app/shared/service/pagination";
import { RouteService } from "src/app/shared/service/route/route.service";
import { DummyWebsocket } from "src/app/shared/service/test/dummywebsocket";
import { Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { EdgeSettings } from "../../edge/edge";
import { EdgeConfig } from "../../edge/edgeconfig";
import { DummyConfig, Component as TComponent } from "../../edge/edgeconfig.spec";
import { newNavigationRoutes } from "../navigation-routing.module";
import { NavigationId, NavigationTree } from "../shared";
import { NavigationService } from "./navigation.service";

describe("NavigationService", () => {
    const dummConfigComponents: TComponent[] = [
        // common
        DummyConfig.Component.SUM(),
        DummyConfig.Component.GOODWE_CHARGER_MPPT_TWO_STRING("charger0"),
        DummyConfig.Component.GOODWE_CHARGER_MPPT_TWO_STRING("charger1"),
        DummyConfig.Component.SOLAR_EDGE_PV_INVERTER("pvInverter0"),
        DummyConfig.Component.SOLAR_EDGE_PV_INVERTER("pvInverter1"),
        DummyConfig.Component.GOODWE_GRID_METER("meter0"),

        // Singleton controllers
        DummyConfig.Component.CONTROLLER_ESS_EMERGENCY_CAPACITY_RESERVE({}),
        DummyConfig.Component.ESS_LIMITER_14A("ctrlEssLimiter14a0"),
        DummyConfig.Component.ESS_RCR("ctrlEssRippleControlReceiver0"),
        DummyConfig.Component.SCHEDULER_JS_CALENDAR(),
        DummyConfig.Component.CONTROLLER_ESS_TIME_OF_USE_TARIFF(),
        DummyConfig.Component.CONTROLLER_ESS_GRIDOPTIMIZEDCHARGE("ctrlEssGridOptimizedCharge0"),

        // Peakshaving
        DummyConfig.Component.CONTROLLER_SYMMETRIC_PEAKSHAVING(),
        DummyConfig.Component.CONTROLLER_SYMMETRIC_PEAKSHAVING("ctrlPeakShaving1"),
        DummyConfig.Component.CONTROLLER_ASYMMETRIC_PEAKSHAVING("ctrlPeakShaving2"),
        DummyConfig.Component.CONTROLLER_ASYMMETRIC_PEAKSHAVING("ctrlPeakShaving3"),
        DummyConfig.Component.CONTROLLER_TIMESLOT_PEAKSHAVING("ctrlTimeslotPeakShaving0"),
        DummyConfig.Component.CONTROLLER_TIMESLOT_PEAKSHAVING("ctrlTimeslotPeakShaving1"),

        // Evcs
        DummyConfig.Component.EVCS_HARDY_BARTH("evcs0"),
        DummyConfig.Component.EVCS_HARDY_BARTH("evcs1"),

        // Evse
        DummyConfig.Component.EVSE_CONTROLLER_SINGLE("ctrlEvseSingle0"),
        DummyConfig.Component.EVSE_CONTROLLER_SINGLE("ctrlEvseSingle1"),
        DummyConfig.Component.EVSE_CHARGEPOINT_KEBA_UDP("evseChargePoint0"),
        DummyConfig.Component.EVSE_CHARGEPOINT_KEBA_UDP("evseChargePoint1"),
        DummyConfig.Component.EVSE_ELECTRIC_VEHICLE_GENERIC(),
        DummyConfig.Component.EVSE_ELECTRIC_VEHICLE_GENERIC("evseElectricVehicle1"),

        // Heat
        DummyConfig.Component.HEAT_PUMP_SG_READY("ctrlIoHeatPump0"),
        DummyConfig.Component.HEAT_PUMP_SG_READY("ctrlIoHeatPump1"),
        DummyConfig.Component.HEAT_MYPV_ACTHOR("heat0"),
        DummyConfig.Component.HEAT_MYPV_ACTHOR("heat1"),
        DummyConfig.Component.CONTROLLER_CLEVER_PV("ctrlCleverPv0"),
        DummyConfig.Component.CONTROLLER_CLEVER_PV("ctrlCleverPv1"),
        DummyConfig.Component.CONTROLLER_IO_HEATINGELEMENT(),
        DummyConfig.Component.CONTROLLER_IO_HEATINGELEMENT("ctrlIoHeatingElement1"),

        // Storage
        DummyConfig.Component.CONTROLLER_ESS_FIXACTIVEPOWER(),
        DummyConfig.Component.CONTROLLER_ESS_FIXACTIVEPOWER("ctrlEssFixActivePower1"),

        // Consumption Meters
        DummyConfig.Component.METER_MICROCARE_SDM630("meter1"),
        DummyConfig.Component.METER_MICROCARE_SDM630("meter2"),

        // Systems
        DummyConfig.Component.SYSTEM_FENECON_INDUSTRIAL_XL(),
        DummyConfig.Component.SYSTEM_FENECON_INDUSTRIAL_L("system1"),
        DummyConfig.Component.SYSTEM_FENECON_INDUSTRIAL_M("system2"),
        DummyConfig.Component.SYSTEM_FENECON_INDUSTRIAL_S("system3"),

        // Others
        DummyConfig.Component.MODBUS_TCP_READWRITE("ctrlModbusTcpReadWrite0"),
        DummyConfig.Component.MODBUS_TCP_READWRITE("ctrlModbusTcpReadWrite1"),
        DummyConfig.Component.CONTROLLER_BRAIINS_SINGLE(),
        DummyConfig.Component.CONTROLLER_BRAIINS_SINGLE("ctrlBraiinsSingle1"),
        DummyConfig.Component.CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlIoFixDigitalOutput0"),
        DummyConfig.Component.CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlIoFixDigitalOutput1"),
        DummyConfig.Component.CONTROLLER_SYMMETRIC_FIXREACTIVEPOWER(),
        DummyConfig.Component.CONTROLLER_SYMMETRIC_FIXREACTIVEPOWER("ctrlFixReactivePower1"),
        DummyConfig.Component.CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD(),
        DummyConfig.Component.CONTROLLER_IO_CHANNEL_SINGLE_THRESHOLD("ctrlIoChannelSingleThreshold1"),
        DummyConfig.Component.CONTROLLER_CHP_SOC(),
        DummyConfig.Component.CONTROLLER_CHP_SOC("ctrlChpSoc1"),
    ];

    let platFormService: PlatFormService;
    let translateService: TranslateService;
    const user = new User("", "test.user", "admin", Language.DE.key, true, {});

    let service: jasmine.SpyObj<Service>;
    const edgeConfig: (components: TComponent[]) => EdgeConfig = (components: TComponent[] = []) =>
        DummyConfig.from(...dummConfigComponents.concat(components));

    beforeEach(async () => {
        service = jasmine.createSpyObj<Service>("Service", ["getCurrentEdge"], {});
        await TestBed.configureTestingModule({
            declarations: [],
            imports: [TranslateModule.forRoot(), PipeComponentsModule],
            providers: [
                { provide: Service, useValue: service },
                { provide: AngularDelegate, useValue: {} },
                { provide: Websocket, useValue: DummyWebsocket },
                TranslateService,
                PlatFormService,
                RouteService,
                Pagination,
                ModalController,
                provideRouter([...toProbeRoutes(routes)]),
            ],
        }).compileComponents();
        platFormService = TestBed.inject(PlatFormService);
        translateService = TestBed.inject(TranslateService);
    });

    describe("+areIdsUnique", () => {
        it("all NavigationTree['id']s are unique", async () => {
            const edge = DummyConfig.dummyEdge({});
            edge["_config"] = signal(edgeConfig([]));
            const navigationTree = await NavigationService.createNavigationTree(
                translateService,
                edge,
                service,
                user,
                platFormService,
            );

            /**
             * Exclude Favorites, this unit test was created exactly for this purpose, to only have unique ids, that can
             * be reused for the favorites
             */
            if (navigationTree != null) {
                navigationTree.children = navigationTree.children.filter((el) => el.id !== NavigationId.FAVORITES);
                expect(NavigationTree.areIdsUnique(navigationTree)).toBeTrue();
            }
        });

        it("all NavigationTree['id']s are unique with duplicate component ids", async () => {
            const edge = DummyConfig.dummyEdge({});
            edge["_config"] = signal(edgeConfig(dummConfigComponents));
            const navigationTree = await NavigationService.createNavigationTree(
                translateService,
                edge,
                service,
                user,
                platFormService,
            );
            /**
             * Exclude Favorites, this unit test was created exactly for this purpose, to only have unique ids, that can
             * be reused for the favorites
             */
            if (navigationTree != null) {
                navigationTree.children = navigationTree.children.filter((el) => el.id !== NavigationId.FAVORITES);
                expect(NavigationTree.areIdsUnique(navigationTree)).toBeTrue();
            }
        });
    });

    describe("are all navigation tree routerLink existing in predefined routes", () => {
        it("all routes are existing", async () => {
            const harness = await RouterTestingHarness.create();

            const edge = DummyConfig.dummyEdge({});
            edge["_config"] = signal(edgeConfig([]));
            const navigationTree = await NavigationService.createNavigationTree(
                translateService,
                edge,
                service,
                user,
                platFormService,
            );

            await testRoutes(navigationTree, harness);
        }, 2000 /* Timeout for angular application to get stable*/);
    });

    describe("are all navigation tree routerLink existing in favorites routes", () => {
        it("all routes are existing", async () => {
            const harness = await RouterTestingHarness.create();

            const edge = DummyConfig.dummyEdge({});
            edge["_config"] = signal(edgeConfig([]));
            const baseNavigationTree = await NavigationService.createNavigationTree(
                translateService,
                edge,
                service,
                user,
                platFormService,
            );

            const allTrees = baseNavigationTree?.getAbsoluteNavigations() ?? [];
            const favoriteTreeIds = allTrees.filter((el) => el.hideFavorite == false).map((el) => el.id);
            edge.settings = { [EdgeSettings.FAVORITES]: { includes: favoriteTreeIds, excludes: [] } };
            const updatedNavigationTree = await NavigationService.createNavigationTree(
                translateService,
                edge,
                service,
                user,
                platFormService,
            );

            await testRoutes(updatedNavigationTree, harness);
        }, 2000 /* Timeout for angular application to get stable*/);
    });
});

/**
 * Test routes of a navigation tree by navigating to each route and checking if it exists in the router.
 *
 * @param navigationTree The navigation tree
 * @param harness The routing harness
 */
async function testRoutes(navigationTree: NavigationTree | null, harness: RouterTestingHarness) {
    const allUpdatedNavigationTrees = navigationTree?.getAbsoluteNavigations() ?? [];
    const failedRoutes: string[] = [];

    for (const route of allUpdatedNavigationTrees) {
        const queryString = route.routerLink.queryParams
            ? `?${new URLSearchParams(route.routerLink.queryParams).toString()}`
            : "";

        const routerLink = (route.customLink ?? route.routerLink.baseString) + queryString;
        try {
            await harness.navigateByUrl(routerLink);
        } catch (error) {
            failedRoutes.push(`${route.id}: ${routerLink} ${error}`);
        }
    }

    expect(failedRoutes).toEqual([]);
}

function toProbeRoutes(routes: Routes, parentPath = ""): Routes {
    return routes.map((route): Route => {
        if (route.redirectTo != null) {
            return {
                path: route.path,
                pathMatch: route.pathMatch,
                redirectTo: route.redirectTo,
            };
        }

        const children = getResolvedChildren(route, parentPath);

        return {
            path: route.path,
            matcher: route.matcher,
            pathMatch: route.pathMatch,
            children,
            component: RouteProbeComponent,
            canActivate: [],
            canActivateChild: [],
            canDeactivate: [],
            canMatch: [],
            resolve: {},
        };
    });
}

@Component({
    standalone: true,
    template: "",
})
class RouteProbeComponent {}

/* Unfortunately there is no way to inject lazy loaded routes synchronously and throughout unit tests */
function getResolvedChildren(route: Route, parentPath: string): Routes | undefined {
    const fullPath = [parentPath, route.path ?? ""]
        .filter((part) => part.length > 0)
        .join("/")
        .replace(/\/+/g, "/");

    if (route.children != null) {
        return toProbeRoutes(route.children, fullPath);
    }

    if (route.loadChildren == null) {
        return undefined;
    }

    // No way around this
    switch (fullPath) {
        case "device/:edgeId":
        case "device/:edgeId/live":
        case "device/:edgeId/favorites":
            return toProbeRoutes(newNavigationRoutes, fullPath);

        case "device/:edgeId/settings":
        case "device/:edgeId/profile/settings":
        case "device/:edgeId/live/settings":
            return toProbeRoutes(settingsRoutes, fullPath);

        default:
            return undefined;
    }
}
