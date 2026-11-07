import { signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, convertToParamMap, Router } from "@angular/router";
import { BehaviorSubject } from "rxjs";
import { DummyConfig } from "../shared/components/edge/edgeconfig.spec";
import { Pagination } from "../shared/service/pagination";
import { RouteService } from "../shared/service/route/route.service";
import { Service, Websocket } from "../shared/shared";
import { States } from "../shared/states/states";
import { EdgeComponent } from "./edge.component";

describe("EdgeComponent route changes", () => {
    it("should select and subscribe to a new edge without recreating the component", async () => {
        const params = new BehaviorSubject(convertToParamMap({ edgeId: "edge0" }));
        const route = { paramMap: params, snapshot: { paramMap: params.value } };
        const service = jasmine.createSpyObj<Service>("Service", ["updateCurrentEdge"], {
            currentEdge: signal(DummyConfig.dummyEdge({})),
        });
        const websocket = { state: signal(States.EDGE_SUBSCRIBED) };
        const pagination = jasmine.createSpyObj<Pagination>("Pagination", ["subscribeEdge"]);
        pagination.subscribeEdge.and.callFake(async () => {
            expect(websocket.state()).toBe(States.EDGE_SELECTED);
            websocket.state.set(States.EDGE_SUBSCRIBED);
        });
        service.updateCurrentEdge.and.callFake(async (edgeId: string) => DummyConfig.dummyEdge({ edgeId }));
        TestBed.configureTestingModule({
            providers: [
                { provide: Service, useValue: service },
                { provide: Websocket, useValue: websocket },
                { provide: Pagination, useValue: pagination },
                { provide: ActivatedRoute, useValue: route },
                { provide: Router, useValue: jasmine.createSpyObj<Router>("Router", ["navigate"]) },
                { provide: RouteService, useValue: { getRouteParam: () => null } },
            ],
        });
        const component = TestBed.runInInjectionContext(
            () =>
                new EdgeComponent(
                    TestBed.inject(RouteService),
                    service,
                    TestBed.inject(Websocket),
                    pagination,
                    TestBed.inject(Router),
                    TestBed.inject(ActivatedRoute),
                ),
        );
        await component.ngOnInit();
        expect(pagination.subscribeEdge.calls.mostRecent().args[0]?.id).toBe("edge0");

        route.snapshot.paramMap = convertToParamMap({ edgeId: "edge1" });
        params.next(route.snapshot.paramMap);
        await Promise.resolve();
        expect(service.updateCurrentEdge.calls.allArgs()).toEqual([["edge0"], ["edge1"]]);
        expect(pagination.subscribeEdge.calls.mostRecent().args[0]?.id).toBe("edge1");

        params.next(route.snapshot.paramMap);
        expect(service.updateCurrentEdge).toHaveBeenCalledTimes(2);
        TestBed.resetTestingModule();
        params.next(convertToParamMap({ edgeId: "edge2" }));
        expect(service.updateCurrentEdge).toHaveBeenCalledTimes(2);
    });
});
