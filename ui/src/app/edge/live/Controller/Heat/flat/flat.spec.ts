import { NO_ERRORS_SCHEMA, signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Service } from "src/app/shared/service/service";
import { UserService } from "src/app/shared/service/user.service";
import { Websocket } from "src/app/shared/service/websocket";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { HeatStatus } from "../shared/shared";
import { ControllerHeatComponent } from "./flat";

/**
 * Minimal unit tests for {@link ControllerHeatComponent}.
 *
 * The component extends {@link AbstractFlatWidget}, which has a heavy DI graph (Websocket, Service, ModalController,
 * ...). To keep these tests minimal we exercise the two pieces of pure logic directly via the prototype, supplying a
 * stub `this` context with only the fields the methods read.
 */
describe("ControllerHeatComponent", () => {
    function callGetChannelAddresses(component: EdgeConfig.Component | null): ChannelAddress[] {
        const stub = { component } as ControllerHeatComponent;
        return (ControllerHeatComponent.prototype as any).getChannelAddresses.call(stub);
    }

    function callOnCurrentData(component: EdgeConfig.Component, currentData: CurrentData): HeatStatus | null {
        const stub: any = Object.create(ControllerHeatComponent.prototype);
        stub.component = component;
        stub.displayStatus = null;
        (ControllerHeatComponent.prototype as any).onCurrentData.call(stub, currentData);
        return stub.displayStatus;
    }

    describe("#getChannelAddresses()", () => {
        it("returns an empty list when component is null", () => {
            expect(callGetChannelAddresses(null)).toEqual([]);
        });

        it("returns the base channels for non-Askoma Heat components", () => {
            const component = new EdgeConfig.Component("heat0", "Heat", true, false, "Heat.MyPv.AcThor9s", {});

            const channels = callGetChannelAddresses(component);

            expect(channels).toEqual([
                new ChannelAddress("heat0", "Status"),
                new ChannelAddress("heat0", "ControlNotAllowed"),
                new ChannelAddress("heat0", "ActivePower"),
                new ChannelAddress("heat0", "Temperature"),
            ]);
        });

        it("includes the _PropertyMode channel for Askoma components", () => {
            const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});

            const channels = callGetChannelAddresses(component);

            expect(channels).toEqual([
                new ChannelAddress("heat0", "Status"),
                new ChannelAddress("heat0", "ControlNotAllowed"),
                new ChannelAddress("heat0", "ActivePower"),
                new ChannelAddress("heat0", "Temperature"),
                new ChannelAddress("heat0", "_PropertyMode"),
            ]);
        });
    });

    describe("#onCurrentData()", () => {
        const component = new EdgeConfig.Component("heat0", "Heat", true, false, "Heat.MyPv.AcThor9s", {});

        function dataWith(values: CurrentData["allComponents"]): CurrentData {
            return { allComponents: values } as CurrentData;
        }

        it("maps heating-like backend states to EXCESS for the legacy heat display converter", () => {
            for (const status of [HeatStatus.STANDBY, HeatStatus.EXCESS, HeatStatus.CONTROL_NOT_ALLOWED]) {
                const result = callOnCurrentData(component, dataWith({ "heat0/Status": status }));

                expect(result).toBe(HeatStatus.EXCESS);
            }
        });

        it("keeps TEMPERATURE_REACHED as the display status", () => {
            const result = callOnCurrentData(component, dataWith({ "heat0/Status": HeatStatus.TEMPERATURE_REACHED }));

            expect(result).toBe(HeatStatus.TEMPERATURE_REACHED);
        });

        it("maps NO_CONTROL_SIGNAL with positive ActivePower to EXCESS for display", () => {
            const result = callOnCurrentData(
                component,
                dataWith({
                    "heat0/Status": HeatStatus.NO_CONTROL_SIGNAL,
                    "heat0/ActivePower": 500,
                }),
            );

            expect(result).toBe(HeatStatus.EXCESS);
        });

        it("keeps NO_CONTROL_SIGNAL as the display status when ActivePower is zero", () => {
            const result = callOnCurrentData(
                component,
                dataWith({
                    "heat0/Status": HeatStatus.NO_CONTROL_SIGNAL,
                    "heat0/ActivePower": 0,
                }),
            );

            expect(result).toBe(HeatStatus.NO_CONTROL_SIGNAL);
        });

        it("maps ERROR to NO_CONTROL_SIGNAL for display", () => {
            const result = callOnCurrentData(component, dataWith({ "heat0/Status": HeatStatus.ERROR }));

            expect(result).toBe(HeatStatus.NO_CONTROL_SIGNAL);
        });

        it("maps a missing Status channel to NO_CONTROL_SIGNAL for display", () => {
            const result = callOnCurrentData(component, dataWith({}));

            expect(result).toBe(HeatStatus.NO_CONTROL_SIGNAL);
        });

        it("maps unknown status numbers to NO_CONTROL_SIGNAL for display", () => {
            const result = callOnCurrentData(component, dataWith({ "heat0/Status": 99 }));

            expect(result).toBe(HeatStatus.NO_CONTROL_SIGNAL);
        });
    });

    describe("template", () => {
        let fixture: ComponentFixture<ControllerHeatComponent>;

        beforeEach(async () => {
            await TestBed.configureTestingModule({
                declarations: [ControllerHeatComponent],
                imports: [TranslateModule.forRoot()],
                providers: [
                    FormBuilder,
                    { provide: ActivatedRoute, useValue: {} },
                    {
                        provide: DataService,
                        useValue: {
                            currentValue: signal({ allComponents: {} }),
                            subscribeChannels: () => {},
                            unsubscribeFromChannels: () => {},
                        },
                    },
                    { provide: ModalController, useValue: {} },
                    { provide: Router, useValue: {} },
                    { provide: Service, useValue: { getCurrentEdge: () => new Promise(() => {}) } },
                    { provide: UserService, useValue: { isNewNavigation: signal(false) } },
                    { provide: Websocket, useValue: {} },
                ],
                schemas: [NO_ERRORS_SCHEMA],
            }).compileComponents();
        });

        it("binds the display status to the status line instead of the raw backend status", () => {
            fixture = TestBed.createComponent(ControllerHeatComponent);
            const component = fixture.componentInstance as any;
            component.ngOnInit = () => {};
            component.isInitialized = true;
            component.component = new EdgeConfig.Component("heat0", "Heat", true, false, "Heat.MyPv.AcThor9s", {});
            component.modalComponent = null;
            component.displayStatus = HeatStatus.EXCESS;

            fixture.detectChanges();

            const statusLine = fixture.nativeElement.querySelector("oe-flat-widget-line");
            expect(statusLine.value).toBe(HeatStatus.EXCESS);
        });
    });
});
