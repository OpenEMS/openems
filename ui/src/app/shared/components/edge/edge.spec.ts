import { TestBed } from "@angular/core/testing";

import { JsonrpcResponseSuccess } from "../../jsonrpc/base";
import { GetEdgeConfigResponse } from "../../jsonrpc/response/getEdgeConfigResponse";
import { GetPropertiesOfFactoryResponse } from "../../jsonrpc/response/getPropertiesOfFactoryResponse";
import { Websocket } from "../../shared";
import { EdgeConfig } from "./edgeconfig";
import { DummyConfig } from "./EDGECONFIG.SPEC";

describe("Edge", () => {
    const websocketSpyObject = JASMINE.CREATE_SPY_OBJ<Websocket>("Websocket", ["sendRequest"]);

    let websocket: Websocket;
    beforeEach(() => {
        TEST_BED.CONFIGURE_TESTING_MODULE({
            providers: [
                { provide: Websocket, useValue: websocketSpyObject },
            ],
        });
        websocket = TEST_BED.INJECT(Websocket);
    });

    it("#getFactoryPropertiesOldVersion", async () => {
        const edge = DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.1.1" });

        const dummyConfig = DUMMY_CONFIG.FROM(DUMMY_CONFIG.COMPONENT.EVCS_KEBA_KECONTACT("evcs0"));
        DUMMY_CONFIG.FACTORIES[DUMMY_CONFIG.FACTORY.EVCS_KEBA_KECONTACT.id].PROPERTIES.PUSH(new EDGE_CONFIG.FACTORY_PROPERTY());
        WEBSOCKET_SPY_OBJECT.SEND_REQUEST.AND.RESOLVE_TO(new JsonrpcResponseSuccess("", {
            payload: new GetEdgeConfigResponse("", dummyConfig),
        }));

        const [factory, properties] = await EDGE.GET_FACTORY_PROPERTIES(websocket, DUMMY_CONFIG.FACTORY.EVCS_KEBA_KECONTACT.id);
        expect(FACTORY.ID).toBe(DUMMY_CONFIG.FACTORY.EVCS_KEBA_KECONTACT.id);
        expect(properties).toBe(DUMMY_CONFIG.FACTORIES[DUMMY_CONFIG.FACTORY.EVCS_KEBA_KECONTACT.id].properties);
    });

    it("#getFactoryPropertiesNewVersion", async () => {
        const edge = DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.6.1" });

        const dummmyFactory = new EDGE_CONFIG.FACTORY("DUMMY.FACTORY.ID", "description");
        const dummyProperties: EDGE_CONFIG.FACTORY_PROPERTY[] = [new EDGE_CONFIG.FACTORY_PROPERTY()];

        WEBSOCKET_SPY_OBJECT.SEND_REQUEST.AND.RESOLVE_TO(new JsonrpcResponseSuccess("", {
            payload: new GetPropertiesOfFactoryResponse("", {
                factory: dummmyFactory,
                properties: dummyProperties,
            }),
        }));

        const [factory, properties] = await EDGE.GET_FACTORY_PROPERTIES(websocket, "DUMMY.FACTORY.ID");
        expect(factory).toBe(dummmyFactory);
        expect(properties).toBe(dummyProperties);
    });
});
