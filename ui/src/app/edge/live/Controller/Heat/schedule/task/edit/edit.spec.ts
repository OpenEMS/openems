import {CUSTOM_ELEMENTS_SCHEMA} from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { AngularDelegate, ModalController } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { RouteService } from "src/app/shared/service/route.service";
import { Service, Websocket } from "src/app/shared/shared";

import { HeatEditTaskComponent } from "./edit";

describe("ControllerHeatAddTaskComponent", () => {
    let fixture: ComponentFixture<HeatEditTaskComponent>;
    let component: HeatEditTaskComponent;
    const edgeMock = {
        subscribeChannels: jasmine.createSpy("subscribeChannels"),
        unsubscribeFromChannels: jasmine.createSpy("unsubscribeFromChannels"),
        currentData: new Subject<any>(),
    };
    const configMock = {
        components: {},
        getComponentSafely: jasmine.createSpy("getComponentSafely").and.returnValue(null),
    };
    const routeServiceMock = {
        getRouteParam: jasmine.createSpy("getRouteParam").and.returnValue(null),
        currentUrl: () => null,
    };
    const serviceMock = {
        getCurrentEdge: jasmine.createSpy("getCurrentEdge").and.resolveTo(edgeMock as any),
        getConfig: jasmine.createSpy("getConfig").and.resolveTo(configMock as any),
        toast: jasmine.createSpy("toast"),
        websocket: {},
        isSmartphoneResolution: false,
        currentEdge: () => null,
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            teardown: { destroyAfterEach: false },
            imports: [HeatEditTaskComponent, TranslateModule.forRoot()],
            providers: [
                { provide: Websocket, useValue: {} },
                { provide: ActivatedRoute, useValue: {} },
                { provide: Service, useValue: serviceMock },
                { provide: ModalController, useValue: {} },
                { provide: AngularDelegate, useValue: {} },
                { provide: RouteService, useValue: routeServiceMock },
                { provide: NavigationService, useValue: {} },
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(HeatEditTaskComponent);
        component = fixture.componentInstance;
        (component as any).edge = edgeMock;
    });

    it("hides edit-task content when Askoma is read-only", () => {
        (component as any).isAskomaReadOnly = true;
        (component as any).ref.reattach();

        fixture.detectChanges();

        const template = fixture.nativeElement.innerHTML as string;

        expect(template).not.toContain("oe-components-scheduler-edit-task");
    });

    it("renders edit-task content when Askoma is writable", () => {
        (component as any).isAskomaReadOnly = false;
        (component as any).ref.reattach();

        fixture.detectChanges();

        const template = fixture.nativeElement.innerHTML as string;

        expect(template).toContain("oe-components-scheduler-edit-task");
    });
});
