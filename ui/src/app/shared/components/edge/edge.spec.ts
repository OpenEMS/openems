import { TestBed } from "@angular/core/testing";

import { JsonrpcResponseSuccess } from "../../jsonrpc/base";
import { GetPropertiesOfFactoryResponse } from "../../jsonrpc/response/getPropertiesOfFactoryResponse";
import { Websocket } from "../../shared";
import { EdgeConfig } from "./edgeconfig";
import { DummyConfig } from "./edgeconfig.spec";

describe("Edge", () => {
    const websocketSpyObject = jasmine.createSpyObj<Websocket>("Websocket", ["sendRequest"]);

    let websocket: Websocket;
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [{ provide: Websocket, useValue: websocketSpyObject }],
        });
        websocket = TestBed.inject(Websocket);
    });

    it("#getFactoryProperties", async () => {
        const edge = DummyConfig.dummyEdge({ version: "2024.6.1" });

        const dummmyFactory = new EdgeConfig.Factory("dummy.factory.id", "description");
        const dummyProperties: EdgeConfig.FactoryProperty[] = [new EdgeConfig.FactoryProperty()];

        websocketSpyObject.sendRequest.and.resolveTo(
            new JsonrpcResponseSuccess("", {
                payload: new GetPropertiesOfFactoryResponse("", {
                    factory: dummmyFactory,
                    properties: dummyProperties,
                }),
            }),
        );

        const [factory, properties] = await edge.getFactoryProperties(websocket, "dummy.factory.id");
        expect(factory).toBe(dummmyFactory);
        expect(properties).toBe(dummyProperties);
    });
});
