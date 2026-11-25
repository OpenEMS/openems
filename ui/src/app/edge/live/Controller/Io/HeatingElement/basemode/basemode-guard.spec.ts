import { Component, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideRouter, Router } from "@angular/router";
import { RouterTestingHarness } from "@angular/router/testing";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { newNavigationRoutes } from "src/app/shared/components/navigation/navigation-routing.module";
import { Edge, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

@Component({ template: "" })
class BaseModePage {
    public static openings = 0;

    constructor() {
        BaseModePage.openings++;
    }
}

@Component({ template: "" })
class HomePage {}

describe("HeatingElement base mode route guard", () => {
    const path = "controller/heatingelement/:componentId/base-mode";
    const url = "/device/edge1/controller/heatingelement/ctrlIoHeatingElement0/base-mode";
    const homeUrl = "/device/edge1/controller/heatingelement/ctrlIoHeatingElement0";
    let updateCurrentEdge: jasmine.Spy<(edgeId: string) => Promise<Edge>>;

    beforeEach(() => {
        const route = newNavigationRoutes.find((entry) => entry.path === path);
        BaseModePage.openings = 0;
        updateCurrentEdge = jasmine.createSpy("updateCurrentEdge");
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: Service,
                    useValue: {
                        currentEdge: signal(DummyConfig.dummyEdge({ edgeId: "edge0", version: "2026.11.1" })),
                        updateCurrentEdge,
                    },
                },
                provideRouter([
                    {
                        path: "device/:edgeId",
                        children: [
                            { path, component: BaseModePage, canActivate: route?.canActivate },
                            { path: "controller/heatingelement/:componentId", component: HomePage },
                        ],
                    },
                ]),
            ],
        });
    });

    it("registers the guard for all new navigation variants", () => {
        for (const prefix of ["", "common/storage/"]) {
            const route = newNavigationRoutes.find((entry) => entry.path === prefix + path);
            expect(route?.canActivate?.length).toBe(1);
        }
    });

    for (const scenario of [
        { role: Role.ADMIN, version: "2026.11.1", allowed: true },
        { role: Role.ADMIN, version: "2026.10.1", allowed: false },
        { role: Role.OWNER, version: "2026.11.1", allowed: false },
        { role: Role.INSTALLER, version: "2026.11.1", allowed: false },
        { role: Role.ADMIN, version: "unknown", allowed: false },
    ]) {
        it(`checks the routed edge for ${Role[scenario.role]} on ${scenario.version}`, async () => {
            updateCurrentEdge.and.resolveTo(
                DummyConfig.dummyEdge({ edgeId: "edge1", role: scenario.role, version: scenario.version }),
            );
            const harness = await RouterTestingHarness.create();
            await harness.navigateByUrl(url);

            expect(updateCurrentEdge).toHaveBeenCalledWith("edge1");
            expect(BaseModePage.openings).toBe(scenario.allowed ? 1 : 0);
            expect(TestBed.inject(Router).url).toBe(scenario.allowed ? url : homeUrl);
        });
    }

    it("does not open the page before the edge role and firmware are resolved", async () => {
        let resolveEdge!: (edge: Edge) => void;
        let requested!: () => void;
        const requestStarted = new Promise<void>((resolve) => {
            requested = resolve;
        });
        updateCurrentEdge.and.callFake(() => {
            requested();
            return new Promise<Edge>((resolve) => {
                resolveEdge = resolve;
            });
        });
        const harness = await RouterTestingHarness.create();
        const navigation = harness.navigateByUrl(url);
        await requestStarted;

        expect(BaseModePage.openings).toBe(0);
        resolveEdge(DummyConfig.dummyEdge({ edgeId: "edge1", role: Role.ADMIN, version: "2026.11.1" }));
        await navigation;
        expect(BaseModePage.openings).toBe(1);
    });

    it("denies a missing role instead of treating null as ADMIN", async () => {
        const edge = DummyConfig.dummyEdge({ edgeId: "edge1", version: "2026.11.1" });
        Object.defineProperty(edge, "role", { value: null });
        updateCurrentEdge.and.resolveTo(edge);
        const harness = await RouterTestingHarness.create();
        await harness.navigateByUrl(url);

        expect(BaseModePage.openings).toBe(0);
        expect(TestBed.inject(Router).url).toBe(homeUrl);
    });

    it("redirects when the routed edge cannot be loaded", async () => {
        const error = new Error("Edge not found");
        updateCurrentEdge.and.rejectWith(error);
        const log = spyOn(console, "warn");
        const harness = await RouterTestingHarness.create();
        await harness.navigateByUrl(url);

        expect(BaseModePage.openings).toBe(0);
        expect(TestBed.inject(Router).url).toBe(homeUrl);
        expect(log).toHaveBeenCalledWith(jasmine.any(String), error);
    });
});
