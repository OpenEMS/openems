import { TestBed } from "@angular/core/testing";
import { ModalController } from "@ionic/angular";
import { DummyService } from "../../service/test/dummyservice";
import { DummyWebsocket } from "../../service/test/dummywebsocket";
import { EdgeConfig, Service, Websocket } from "../../shared";
import { DummyModalController } from "../../test/DummyModalController";
import { StatusSingleComponent } from "./status.component";


describe('StatusComponent', () => {
    const testComponent = new EdgeConfig.Component("test", {}, {
        "testChannel": {
            accessMode: "RO",
            category: "ENUM",
            type: "BOOLEAN",
            unit: "W",
            level: "OK"
        }
    });
    let statusComponent: StatusSingleComponent;
    // initialize variables only in beforeEach, beforeAll
    beforeEach((() => {
        TestBed.configureTestingModule({
            // provide the component-under-test and dependent service
            providers: [
                StatusSingleComponent,
                { provide: ModalController, useClass: DummyModalController },
                { provide: Service, useClass: DummyService },
                { provide: Websocket, useClass: DummyWebsocket }
            ]
        });
        statusComponent = TestBed.inject(StatusSingleComponent);
    }));

    it('Test add Channels for subscription', () => {
        statusComponent.ngOnInit();
        statusComponent.subscribeInfoChannels(testComponent);
        expect(statusComponent.subscribedInfoChannels.length).toBe(1);
        statusComponent.subscribedInfoChannels.forEach((channelAddress) => {
            expect(channelAddress.channelId).toBe('testChannel');
        });
    });
});

// TODO dummy classes for needed services
class Dummy {

}