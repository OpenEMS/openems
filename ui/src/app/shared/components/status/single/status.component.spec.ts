import { TestBed } from "@angular/core/testing";
import { ModalController } from "@ionic/angular";
import { DummyWebsocket } from "src/app/shared/service/test/dummywebsocket";
import { ChannelAddress, Service, Websocket } from "src/app/shared/shared";
import { Edge } from "../../edge/edge";
import { EdgeConfig, PersistencePriority } from "../../edge/edgeconfig";
import { DummyConfig } from "../../edge/EDGECONFIG.SPEC";
import { DummyModalController } from "../../shared/testing/DummyModalController";
import { StatusSingleComponent } from "./STATUS.COMPONENT";

describe("StatusComponent", () => {
    const testComponent = new EDGE_CONFIG.COMPONENT("component0", "", true, "test", {}, {
        "testChannel": {
            accessMode: "RO",
            category: "STATE",
            type: "BOOLEAN",
            unit: "W",
            level: "OK",
            persistencePriority: PERSISTENCE_PRIORITY.HIGH,
            text: "",
        },
    });
    TEST_COMPONENT.ID = "test0";

    let statusComponent: StatusSingleComponent;
    const serviceSpy = JASMINE.CREATE_SPY_OBJ("Service", ["getConfig", "getCurrentEdge"], ["currentEdge"],);
    const edgeSpy: Edge = JASMINE.CREATE_SPY_OBJ("Edge", ["subscribeChannels", "isVersionAtLeast", "unsubscribeChannels"]);
    const edgeConfigSpy = JASMINE.CREATE_SPY_OBJ("EdgeConfig", ["listActiveComponents"], ["components"]);
    // initialize variables only in beforeEach, beforeAll
    beforeEach((() => {
        TEST_BED.CONFIGURE_TESTING_MODULE({
            // provide the component-under-test and dependent service
            providers: [
                StatusSingleComponent,
                { provide: ModalController, useClass: DummyModalController },
                { provide: Service, useValue: serviceSpy },
                { provide: Websocket, useClass: DummyWebsocket },
                { provide: Edge, useValue: edgeSpy },
                { provide: EdgeConfig, useValue: edgeConfigSpy },
            ],
        });
        const valueEdgeSpy = TEST_BED.INJECT(Edge) as JASMINE.SPY_OBJ<Edge>;
        VALUE_EDGE_SPY.IS_VERSION_AT_LEAST.AND.RETURN_VALUE(false); // check should be false then
        VALUE_EDGE_SPY.UNSUBSCRIBE_CHANNELS.AND.CALL_THROUGH();

        const valueServiceSpy = TEST_BED.INJECT(Service) as JASMINE.SPY_OBJ<Service>;
        VALUE_SERVICE_SPY.GET_CURRENT_EDGE.AND.RETURN_VALUE(PROMISE.RESOLVE(DUMMY_CONFIG.DUMMY_EDGE({})));

        const valueEdgeConfigSpy = TEST_BED.INJECT(EdgeConfig) as JASMINE.SPY_OBJ<EdgeConfig>;
        VALUE_EDGE_CONFIG_SPY.LIST_ACTIVE_COMPONENTS.AND.RETURN_VALUE([{ category: { icon: "", title: "title" }, components: [testComponent] }]);
        spyPropertyGetter(valueEdgeConfigSpy, "components").AND.RETURN_VALUE({ [TEST_COMPONENT.ID]: testComponent });
        VALUE_SERVICE_SPY.GET_CONFIG.AND.RESOLVE_TO(TEST_BED.INJECT(EdgeConfig));

        statusComponent = TEST_BED.INJECT(StatusSingleComponent);
    }));

    it("Test add Channels for subscription", async () => {
        await STATUS_COMPONENT.NG_ON_INIT();
        await STATUS_COMPONENT.SUBSCRIBE_INFO_CHANNELS(testComponent);
        expect(STATUS_COMPONENT.SUBSCRIBED_INFO_CHANNELS).toHaveSize(2);
        expect(STATUS_COMPONENT.SUBSCRIBED_INFO_CHANNELS).toContain(new ChannelAddress(TEST_COMPONENT.ID, "State"));
        expect(STATUS_COMPONENT.SUBSCRIBED_INFO_CHANNELS).toContain(new ChannelAddress(TEST_COMPONENT.ID, "testChannel"));
    });
});

// TODO should be some common method
function spyPropertyGetter<T, K extends keyof T>(
    spyObj: JASMINE.SPY_OBJ<T>,
    propName: K,
): JASMINE.SPY<() => T[K]> {
    return OBJECT.GET_OWN_PROPERTY_DESCRIPTOR(spyObj, propName)?.get as JASMINE.SPY<() => T[K]>;
}
