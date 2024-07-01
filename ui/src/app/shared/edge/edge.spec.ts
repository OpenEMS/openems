
import { TestBed } from "@angular/core/testing";
import { DummyConfig } from "./edgeconfig.spec";
import { EdgeConfig, Websocket } from "../shared";
import { GetPropertiesOfFactoryResponse } from "../jsonrpc/response/getPropertiesOfFactoryResponse";
import { JsonrpcResponseSuccess } from "../jsonrpc/base";
import { GetEdgeConfigResponse } from "../jsonrpc/response/getEdgeConfigResponse";

describe('Edge', () => {
    const websocketSpyObject = jasmine.createSpyObj<Websocket>('Websocket', ['sendRequest']);

    let websocket: Websocket;
    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                { provide: Websocket, useValue: websocketSpyObject },
            ],
        });
        websocket = TestBed.inject(Websocket);
    });

    it('#getFactoryPropertiesOldVersion', async () => {
        const edge = DummyConfig.dummyEdge({ version: '2024.1.1' });

        const dummyConfig = DummyConfig.from(DummyConfig.Component.EVCS_KEBA_KECONTACT('evcs0'));
        dummyConfig.factories[DummyConfig.Factory.EVCS_KEBA_KECONTACT.id].properties.push(new EdgeConfig.FactoryProperty());
        websocketSpyObject.sendRequest.and.resolveTo(new JsonrpcResponseSuccess('', {
            payload: new GetEdgeConfigResponse('', dummyConfig),
        }));

        const [factory, properties] = await edge.getFactoryProperties(websocket, DummyConfig.Factory.EVCS_KEBA_KECONTACT.id);
        expect(factory.id).toBe(DummyConfig.Factory.EVCS_KEBA_KECONTACT.id);
        expect(properties).toBe(dummyConfig.factories[DummyConfig.Factory.EVCS_KEBA_KECONTACT.id].properties);
    });

    it('#getFactoryPropertiesNewVersion', async () => {
        const edge = DummyConfig.dummyEdge({ version: '2024.6.1' });

        const dummmyFactory = new EdgeConfig.Factory('dummy.factory.id', 'description');
        const dummyProperties: EdgeConfig.FactoryProperty[] = [new EdgeConfig.FactoryProperty()];

        websocketSpyObject.sendRequest.and.resolveTo(new JsonrpcResponseSuccess('', {
            payload: new GetPropertiesOfFactoryResponse('', {
                factory: dummmyFactory,
                properties: dummyProperties,
            }),
        }));

        const [factory, properties] = await edge.getFactoryProperties(websocket, 'dummy.factory.id');
        expect(factory).toBe(dummmyFactory);
        expect(properties).toBe(dummyProperties);
    });
});
