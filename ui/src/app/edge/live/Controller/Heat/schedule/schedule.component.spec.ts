import { CUSTOM_ELEMENTS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ActivatedRoute } from "@angular/router";
import { AngularDelegate, ModalController } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { EdgeConfig, Service, Websocket } from "src/app/shared/shared";

import { HeatScheduleComponent } from "./schedule.component";

describe("ControllerHeatScheduleComponent", () => {
    let fixture: ComponentFixture<HeatScheduleComponent>;
    let component: HeatScheduleComponent;
    let routeServiceMock: { getRouteParam: jasmine.Spy<(paramName: string) => string>; currentUrl: () => null; };

    beforeEach(async () => {
        routeServiceMock = {
            getRouteParam: jasmine.createSpy("getRouteParam").and.returnValue("heat0"),
            currentUrl: () => null,
        };

        await TestBed.configureTestingModule({
            teardown: { destroyAfterEach: false },
            imports: [HeatScheduleComponent, TranslateModule.forRoot()],
            providers: [
                { provide: Websocket, useValue: {} },
                { provide: ActivatedRoute, useValue: {} },
                { provide: Service, useValue: {} },
                { provide: ModalController, useValue: {} },
                { provide: AngularDelegate, useValue: {} },
                { provide: RouteService, useValue: routeServiceMock },
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
        }).compileComponents();

        fixture = TestBed.createComponent(HeatScheduleComponent);
        component = fixture.componentInstance;
        (component as any).edge = {
            unsubscribeFromChannels: jasmine.createSpy("unsubscribeFromChannels"),
        };
    });

    it("isAskomaReadOnly should be true when component is Heat.Askoma and readonly", () => {
        applyComponentConfig(createHeatComponent("Heat.Askoma", true));

        expect((component as any).isAskomaReadOnly).toBeTrue();
    });

    it("isAskomaReadOnly should be false when component is Heat.Askoma but not readonly", () => {
        applyComponentConfig(createHeatComponent("Heat.Askoma", false));

        expect((component as any).isAskomaReadOnly).toBeFalse();
    });

    it("isAskomaReadOnly should be false when component is not Heat.Askoma", () => {
        applyComponentConfig(createHeatComponent("Heat.Other", true));

        expect((component as any).isAskomaReadOnly).toBeFalse();
    });

    function applyComponentConfig(heatComponent: EdgeConfig.Component): void {
        const config = {
            getComponentSafely: jasmine.createSpy("getComponentSafely").and.returnValue(heatComponent),
        } as unknown as EdgeConfig;

        (component as any).updateComponent(config);
        (component as any).onIsInitialized();
    }

    function createHeatComponent(factoryId: string, readOnly: boolean): EdgeConfig.Component {
        return new EdgeConfig.Component("heat0", "Heat", true, false, factoryId, { readOnly });
    }
});
