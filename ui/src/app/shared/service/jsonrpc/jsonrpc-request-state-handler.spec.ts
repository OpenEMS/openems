import { Injector, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { ActivatedRoute, convertToParamMap, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { of } from "rxjs";
import { EdgeComponent } from "src/app/edge/edge.component";
import { JsonrpcRequest } from "../../jsonrpc/base";
import { EdgeRpcRequest } from "../../jsonrpc/request/edgeRpcRequest";
import { States } from "../../states/states";
import { AuthService } from "../auth/auth.service";
import { Pagination } from "../pagination";
import { RouteService } from "../route/route.service";
import { Service } from "../service";
import { UserService } from "../user.service";
import { Websocket } from "../websocket";
import { JsonrpcRequestStateHandler } from "./jsonrpc-request-state-handler";

describe("JsonrpcRequestStateHandler rejection handling", () => {
    let handler: JsonrpcRequestStateHandler;
    let pagination: Pagination;
    let websocket: Websocket;
    let request: EdgeRpcRequest;
    const state = signal(States.EDGE_SELECTED);
    const currentEdge = signal(null);
    const authenticate = jasmine.createSpy("authenticate");
    const reconnectIfNeeded = jasmine.createSpy("reconnectIfNeeded");
    const sendStateLessRequest = jasmine.createSpy("sendStateLessRequest");
    const navigate = jasmine.createSpy("navigate");
    const updateCurrentEdge = jasmine.createSpy("updateCurrentEdge");

    beforeEach(() => {
        state.set(States.EDGE_SELECTED);
        currentEdge.set(null);
        authenticate.and.resolveTo();
        reconnectIfNeeded.and.resolveTo();
        sendStateLessRequest.calls.reset();
        navigate.calls.reset();
        navigate.and.resolveTo(true);
        updateCurrentEdge.and.resolveTo(null);
        handler = new JsonrpcRequestStateHandler();
        const websocketMock = {
            state,
            reconnectIfNeeded,
            sendStateLessRequest,
            jsonRpcRequestHandler: handler,
            injector: null as Injector | null,
        };
        TestBed.configureTestingModule({
            providers: [
                Pagination,
                EdgeComponent,
                { provide: Websocket, useValue: websocketMock },
                { provide: AuthService, useValue: { authenticate } },
                { provide: Service, useValue: { currentEdge, updateCurrentEdge } },
                { provide: Router, useValue: { navigate } },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        paramMap: of(convertToParamMap({ edgeId: "edge0" })),
                        snapshot: { paramMap: convertToParamMap({ edgeId: "edge0" }) },
                    },
                },
                { provide: RouteService, useValue: { getRouteParam: () => "edge0" } },
                { provide: ModalController, useValue: {} },
                { provide: UserService, useValue: {} },
            ],
        });
        websocketMock.injector = TestBed.inject(Injector);
        websocket = TestBed.inject(Websocket);
        pagination = TestBed.inject(Pagination);
        const payload = new JsonrpcRequest("test", {});
        spyOnProperty(payload, "RequiredState").and.returnValue(States.EDGE_SUBSCRIBED);
        request = new EdgeRpcRequest({ edgeId: "edge0", payload });
    });

    it("rejects a missing edge with an Error", async () => {
        await expectAsync(pagination.subscribeEdge(null, websocket)).toBeRejectedWithError(
            "Cannot subscribe to an edge: no edge selected.",
        );
        expect(state()).toBe(States.EDGE_SELECTED);
    });

    it("propagates a missing-edge rejection from state establishment", async () => {
        await expectAsync(handler.establishRequestMinState(request, websocket)).toBeRejectedWithError(
            "Cannot subscribe to an edge: no edge selected.",
        );
    });

    it("rejects a stateful request without sending it when state establishment fails", async () => {
        await expectAsync(Websocket.prototype.sendStateFullRequest.call(websocket, request)).toBeRejectedWithError(
            "Cannot subscribe to an edge: no edge selected.",
        );
        expect(sendStateLessRequest).not.toHaveBeenCalled();
    });

    it("propagates a reconnect rejection", async () => {
        const error = new Error("Connection failed");
        state.set(States.WEBSOCKET_CONNECTION_CLOSED);
        reconnectIfNeeded.and.rejectWith(error);
        await expectAsync(handler.establishRequestMinState(request, websocket)).toBeRejectedWith(error);
    });

    it("propagates an authentication rejection", async () => {
        const error = new Error("Authentication failed");
        authenticate.and.rejectWith(error);
        await expectAsync(handler.establishRequestMinState(request, websocket)).toBeRejectedWith(error);
    });

    it("propagates an edge lookup rejection", async () => {
        const error = new Error("Edge lookup failed");
        spyOn(pagination, "getEdge").and.rejectWith(error);
        await expectAsync(handler.establishRequestMinState(request, websocket)).toBeRejectedWith(error);
    });

    it("resolves without subscribing when the required state is already established", async () => {
        state.set(States.EDGE_SUBSCRIBED);
        const subscribeEdge = spyOn(pagination, "subscribeEdge");
        await expectAsync(handler.establishRequestMinState(request, websocket)).toBeResolved();
        expect(subscribeEdge).not.toHaveBeenCalled();
    });

    it("handles subscription failure during component initialization", async () => {
        const component = TestBed.inject(EdgeComponent);
        await expectAsync(component.ngOnInit()).toBeResolved();
        expect(navigate).toHaveBeenCalledOnceWith(["index"]);
    });

    it("handles edge lookup failure during component initialization", async () => {
        updateCurrentEdge.and.rejectWith(new Error("Edge lookup failed"));
        const component = TestBed.inject(EdgeComponent);
        await expectAsync(component.ngOnInit()).toBeResolved();
        expect(navigate).toHaveBeenCalledOnceWith(["index"]);
    });
});
