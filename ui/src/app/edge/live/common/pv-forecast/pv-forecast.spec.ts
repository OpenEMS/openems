// @ts-strict-ignore
import { signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { addDays, addMinutes, startOfToday } from "date-fns";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { Service, Websocket } from "src/app/shared/shared";
import { PvForecastComponent } from "./pv-forecast";

/**
 * Unit tests for {@link PvForecastComponent}. The logic is exercised via the prototype with a stub `this`, because the
 * constructor relies on `inject()`.
 */
describe("PvForecastComponent", () => {
    function createComponent(overrides: Record<string, any> = {}): any {
        const component: any = Object.create(PvForecastComponent.prototype);
        Object.assign(component, {
            today: null,
            tomorrow: null,
            tomorrowPending: false,
            timer: null,
            edge: () => ({}),
            websocket: {},
            cdRef: { markForCheck: jasmine.createSpy("markForCheck") },
            translate: { setTranslation: jasmine.createSpy("setTranslation") },
            ...overrides,
        });
        return component;
    }

    /**
     * Schedule in quarter hours from today 00:00 until `until`, with 4 kW production from 08:00 to 16:00 every day.
     *
     * @param until End of the schedule (exclusive)
     * @returns The schedule response
     */
    function schedule(until: Date): GetSchedule.Response {
        const data = [];
        for (let t = startOfToday(); t < until; t = addMinutes(t, 15)) {
            const hour = t.getHours();
            data.push({
                timestamp: t.toISOString(),
                type: t <= new Date() ? "HISTORY" : "PREDICTION",
                _sum: { ProductionActivePower: hour >= 8 && hour < 16 ? 4000 : 0 },
                eshs: [],
            });
        }
        return new GetSchedule.Response("id", { data: data });
    }

    beforeEach(() => {
        jasmine.clock().install();
        const noon = startOfToday();
        noon.setHours(12);
        jasmine.clock().mockDate(noon);
    });

    afterEach(() => {
        jasmine.clock().uninstall();
    });

    describe("#load()", () => {
        it("calculates today and tomorrow when the schedule covers tomorrow", async () => {
            spyOn(GetSchedule, "getSchedule").and.returnValue(Promise.resolve(schedule(addDays(startOfToday(), 2))));
            const component = createComponent();
            await component.load();

            expect(component.today).toEqual({ total: 32, remaining: 15 });
            expect(component.tomorrow).toBe(32);
            expect(component.tomorrowPending).toBeFalse();
            expect(component.cdRef.markForCheck).toHaveBeenCalled();
        });

        it("requests the schedule from noon, so that it contains two days", async () => {
            const spy = spyOn(GetSchedule, "getSchedule").and.returnValue(Promise.resolve(schedule(new Date())));
            await createComponent().load();

            expect((spy.calls.mostRecent().args[2] as Date).getHours()).toBe(12);
        });

        it("marks tomorrow as pending when the schedule ends too early", async () => {
            const until = addDays(startOfToday(), 1);
            until.setHours(10);
            spyOn(GetSchedule, "getSchedule").and.returnValue(Promise.resolve(schedule(until)));
            const component = createComponent();
            await component.load();

            expect(component.today.total).toBe(32);
            expect(component.tomorrow).toBeNull();
            expect(component.tomorrowPending).toBeTrue();
        });

        it("hides the forecast without production values", async () => {
            spyOn(GetSchedule, "getSchedule").and.returnValue(
                Promise.resolve(new GetSchedule.Response("id", { data: [] })),
            );
            const component = createComponent({ today: { total: 1, remaining: 0 }, tomorrow: 2 });
            await component.load();

            expect(component.today).toBeNull();
            expect(component.tomorrow).toBeNull();
            expect(component.tomorrowPending).toBeFalse();
        });

        it("hides the forecast when the request fails", async () => {
            spyOn(GetSchedule, "getSchedule").and.returnValue(Promise.reject(new Error("no _energy")));
            const component = createComponent({ today: { total: 1, remaining: 0 } });
            await component.load();

            expect(component.today).toBeNull();
        });

        it("does nothing without edge", async () => {
            const spy = spyOn(GetSchedule, "getSchedule");
            await createComponent({ edge: () => null }).load();

            expect(spy).not.toHaveBeenCalled();
        });
    });

    describe("as component", () => {
        it("loads the forecast once an edge is selected, refreshes it and renders it", async () => {
            const currentEdge = signal(null);
            await TestBed.configureTestingModule({
                imports: [PvForecastComponent, TranslateModule.forRoot()],
                providers: [
                    { provide: Service, useValue: { currentEdge: currentEdge } },
                    { provide: Websocket, useValue: {} },
                ],
            }).compileComponents();
            const getSchedule = spyOn(GetSchedule, "getSchedule").and.callFake(() =>
                Promise.resolve(schedule(addDays(startOfToday(), 2))),
            );
            const fixture = TestBed.createComponent(PvForecastComponent);
            fixture.detectChanges();
            expect(getSchedule).not.toHaveBeenCalled();

            currentEdge.set({});
            fixture.detectChanges();
            await fixture.whenStable();
            fixture.detectChanges();
            expect(getSchedule).toHaveBeenCalledTimes(1);
            expect(fixture.nativeElement.textContent).toContain("EDGE.INDEX.PV_FORECAST.TOMORROW");

            jasmine.clock().tick(15 * 60 * 1000);
            expect(getSchedule).toHaveBeenCalledTimes(2);

            // switching to no edge stops the refresh
            currentEdge.set(null);
            fixture.detectChanges();
            jasmine.clock().tick(15 * 60 * 1000);
            expect(getSchedule).toHaveBeenCalledTimes(2);

            fixture.destroy();
        });
    });

    describe("#trend", () => {
        it("compares tomorrow with today", () => {
            expect(createComponent({ today: { total: 10, remaining: 0 }, tomorrow: 20 }).trend).toBe(1);
            expect(createComponent({ today: { total: 10, remaining: 0 }, tomorrow: 5 }).trend).toBe(-1);
            expect(createComponent({ today: { total: 10, remaining: 0 }, tomorrow: 10.5 }).trend).toBe(0);
            expect(createComponent({ today: null, tomorrow: 10 }).trend).toBe(0);
        });
    });

    describe("#format()", () => {
        it("shows one decimal below 10 kWh", () => {
            const component = createComponent();

            expect(component.format(5.25)).toMatch(/^5[.,]3 kWh$/);
            expect(component.format(12.4)).toBe("12 kWh");
        });
    });

    describe("#registerTranslations()", () => {
        it("adds the German and English texts", async () => {
            const component = createComponent();
            component.registerTranslations();
            await Promise.resolve();
            await Promise.resolve();
            await Promise.resolve();

            expect(component.translate.setTranslation).toHaveBeenCalledWith("de", jasmine.any(Object), true);
            expect(component.translate.setTranslation).toHaveBeenCalledWith("en", jasmine.any(Object), true);
        });
    });

    describe("#ngOnDestroy()", () => {
        it("stops the refresh timer", () => {
            const refresh = jasmine.createSpy("refresh");
            const component = createComponent({ timer: setInterval(refresh, 1000) });
            component.ngOnDestroy();
            jasmine.clock().tick(5000);

            expect(refresh).not.toHaveBeenCalled();
        });
    });
});
