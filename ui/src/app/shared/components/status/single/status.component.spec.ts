import { TestBed } from "@angular/core/testing";
import { ModalController } from "@ionic/angular";

import { EdgeConfig, PersistencePriority } from "../../edge/edgeconfig";
import { StatusSingleComponent } from "./status.component";
import { BehaviorSubject } from "rxjs";
import { DummyWebsocket } from "src/app/shared/service/test/dummywebsocket";
import { Service, Websocket, ChannelAddress } from "src/app/shared/shared";
import { Edge } from "../../edge/edge";
import { DummyModalController } from "../../shared/testing/DummyModalController";

describe('StatusComponent', () => {
    const testComponent = new EdgeConfig.Component("test", {}, {
        "testChannel": {
            accessMode: "RO",
            category: "STATE",
            type: "BOOLEAN",
            unit: "W",
            level: "OK",
            persistencePriority: PersistencePriority.HIGH,
            text: "",
        },
    });
    testComponent.id = 'test0';

    let statusComponent: StatusSingleComponent;
    const serviceSpy = jasmine.createSpyObj('Service', ['getConfig'], ['currentEdge']);
    const edgeSpy = jasmine.createSpyObj('Edge', ['subscribeChannels', 'isVersionAtLeast', 'unsubscribeChannels']);
    const edgeConfigSpy = jasmine.createSpyObj('EdgeConfig', ['listActiveComponents'], ['components']);
    // initialize variables only in beforeEach, beforeAll
    beforeEach((() => {
        TestBed.configureTestingModule({
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
        const valueEdgeSpy = TestBed.inject(Edge) as jasmine.SpyObj<Edge>;
        valueEdgeSpy.isVersionAtLeast.and.returnValue(false); // check should be false then
        valueEdgeSpy.unsubscribeChannels.and.callThrough();

        const valueServiceSpy = TestBed.inject(Service) as jasmine.SpyObj<Service>;
        spyPropertyGetter(valueServiceSpy, 'currentEdge').and.returnValue(new BehaviorSubject(TestBed.inject(Edge)));

        const valueEdgeConfigSpy = TestBed.inject(EdgeConfig) as jasmine.SpyObj<EdgeConfig>;
        valueEdgeConfigSpy.listActiveComponents.and.returnValue([{ category: { icon: '', title: 'title' }, components: [testComponent] }]);
        spyPropertyGetter(valueEdgeConfigSpy, 'components').and.returnValue({ [testComponent.id]: testComponent });
        valueServiceSpy.getConfig.and.resolveTo(TestBed.inject(EdgeConfig));

        statusComponent = TestBed.inject(StatusSingleComponent);
    }));

    it('Test add Channels for subscription', async () => {
        await statusComponent.ngOnInit();
        await statusComponent.subscribeInfoChannels(testComponent);
        expect(statusComponent.subscribedInfoChannels).toHaveSize(2);
        expect(statusComponent.subscribedInfoChannels).toContain(new ChannelAddress(testComponent.id, 'State'));
        expect(statusComponent.subscribedInfoChannels).toContain(new ChannelAddress(testComponent.id, 'testChannel'));
    });
});

// TODO should be some common method
function spyPropertyGetter<T, K extends keyof T>(
    spyObj: jasmine.SpyObj<T>,
    propName: K,
): jasmine.Spy<() => T[K]> {
    return Object.getOwnPropertyDescriptor(spyObj, propName)?.get as jasmine.Spy<() => T[K]>;
}
