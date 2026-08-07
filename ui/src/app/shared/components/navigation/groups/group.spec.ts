import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { TranslateLoader, TranslateModule, TranslateService } from "@ngx-translate/core";
import { MyTranslateLoader } from "src/app/shared/type/language";
import { Service } from "../../../../shared/shared";
import { Widgets } from "../../../../shared/type/widgets";
import { RouteService } from "../../../service/route.service";
import { EdgeConfig } from "../../edge/edgeconfig";
import { ControllerGroupListComponent } from "./group";

describe("ControllerGroupListComponent", () => {
    let component: ControllerGroupListComponent;
    let fixture: ComponentFixture<ControllerGroupListComponent>;

    let service: jasmine.SpyObj<Service>;
    let router: jasmine.SpyObj<Router>;
    let routeService: jasmine.SpyObj<RouteService>;
    let translate: TranslateService;

    const factoryId = "Controller.Io.FixDigitalOutput";

    beforeEach(async () => {
        service = jasmine.createSpyObj<Service>("Service", ["getCurrentEdge"]);

        router = jasmine.createSpyObj<Router>("Router", ["navigate"]);

        routeService = jasmine.createSpyObj<RouteService>("RouteService", ["getQueryParam", "getCurrentUrl"]);

        await TestBed.configureTestingModule({
            imports: [
                ControllerGroupListComponent,
                TranslateModule.forRoot({
                    loader: { provide: TranslateLoader, useClass: MyTranslateLoader },
                }),
            ],
            providers: [
                { provide: Service, useValue: service },
                { provide: Router, useValue: router },
                { provide: RouteService, useValue: routeService },
            ],
        }).compileComponents();

        translate = TestBed.inject(TranslateService);
        translate.addLangs(["de"]);
        translate.use("de");

        fixture = TestBed.createComponent(ControllerGroupListComponent);

        component = fixture.componentInstance;

        router.navigate.and.resolveTo(true);
    });

    it("should not load the edge when factoryId is missing", async () => {
        routeService.getQueryParam.and.returnValue(null);

        fixture.detectChanges();
        await fixture.whenStable();

        expect(service.getCurrentEdge).not.toHaveBeenCalled();
    });

    it("should render enabled components and the grouped title", async () => {
        const enabledComponent1 = {
            id: "component-1",
            alias: "Relay1",
            isEnabled: true,
        } as EdgeConfig.Component;

        const enabledComponent2 = {
            id: "component-2",
            alias: "Relay2",
            isEnabled: true,
        } as EdgeConfig.Component;

        const disabledComponent = {
            id: "component-3",
            alias: "Relay3",
            isEnabled: false,
        } as EdgeConfig.Component;

        const config = jasmine.createSpyObj<EdgeConfig>("EdgeConfig", [
            "getComponentIdsByFactory",
            "getComponentSafely",
        ]);

        config.getComponentIdsByFactory.and.returnValue([
            enabledComponent1.id,
            enabledComponent2.id,
            disabledComponent.id,
        ]);

        const components = new Map<string, EdgeConfig.Component>([
            [enabledComponent1.id, enabledComponent1],
            [enabledComponent2.id, enabledComponent2],
            [disabledComponent.id, disabledComponent],
        ]);

        config.getComponentSafely.and.callFake((componentId: string | null) =>
            componentId == null ? null : (components.get(componentId) ?? null),
        );

        const edge = jasmine.createSpyObj("Edge", ["getCurrentConfig"]);

        edge.getCurrentConfig.and.returnValue(config);

        routeService.getQueryParam.and.returnValue(factoryId);
        service.getCurrentEdge.and.resolveTo(edge as never);

        const groupedFactory = Widgets.GROUPED_FACTORIES[factoryId];

        expect(groupedFactory).toBeDefined();

        const groupedSpy = spyOn(groupedFactory!, "grouped").and.callThrough();

        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();

        expect(config.getComponentIdsByFactory).toHaveBeenCalledWith(factoryId);

        expect(groupedSpy).toHaveBeenCalledWith(
            translate,
            [enabledComponent1.id, enabledComponent2.id],
            config,
            factoryId,
        );

        const title = fixture.nativeElement.querySelector("ion-card-title");

        const expectedTitle = translate.instant("MENU.GROUPS.FIX_DIGITAL_OUTPUT");
        expect(title.textContent.trim()).toBe(expectedTitle);

        const items = fixture.nativeElement.querySelectorAll("ion-item");

        expect(items.length).toBe(2);

        expect(items[0].textContent).toContain("Relay1");
        expect(items[1].textContent).toContain("Relay2");

        expect(fixture.nativeElement.textContent).not.toContain("Relay3");
    });

    it("should remove query parameters before navigating", async () => {
        routeService.getCurrentUrl.and.returnValue("/controller/group?factoryId=Controller.Io.FixDigitalOutput");

        await component.navigateTo("component-1");

        expect(router.navigate).toHaveBeenCalledWith(["/controller/group", "component-1"]);
    });
});
