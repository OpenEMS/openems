import { Injector, computed, runInInjectionContext, signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { DefaultUrlSerializer, Router } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { DeviceDetectorService } from "ngx-device-detector";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { environment } from "src/environments";
import { version as uiVersion } from "../../../../package.json";
import { NavigationService } from "../components/navigation/service/navigation.service";
import { TutorialState, User, UserSettings } from "../jsonrpc/shared";
import { Service } from "../service/service";
import { UserService } from "../service/user.service";
import { NAVIGATION_TOUR, NavigationTour, NavigationTourConfig } from "./navigation-tour";
import { TourStep } from "./tour-step";
import { TourService } from "./tour.service";
import { getActiveTourTarget } from "./tour.utils";
import { TutorialTargetId } from "./tutorial-target-id";

function createVisibleTarget(id: TutorialTargetId): HTMLElement {
    const target = document.createElement("div");
    target.dataset.tutorialId = id;
    target.style.width = "10px";
    target.style.height = "10px";
    document.body.append(target);
    return target;
}

/**
 * Advances the mocked clock in steps of the target lookup interval, letting pending promises settle in between.
 *
 * @param ms Time to advance in milliseconds.
 */
async function advanceTime(ms: number): Promise<void> {
    for (let elapsed = 0; elapsed < ms; elapsed += 20) {
        await new Promise<void>((resolve) => queueMicrotask(resolve));
        await Promise.resolve();
        jasmine.clock().tick(20);
    }
    await Promise.resolve();
    await Promise.resolve();
}

const THEME_TOUR_STEP: TourStep = {
    id: "intro",
    version: "2026.11.1",
    text: "Intro",
    position: "center",
    handbookUrl: "https://docs.example.com/{language}/cockpit.html",
};

let themeTourSteps: TourStep[] = [THEME_TOUR_STEP];

class ThemeNavigationTour implements NavigationTour {
    public getDesktopSteps(): TourStep[] {
        return themeTourSteps;
    }

    public getMobileSteps(): TourStep[] {
        return themeTourSteps;
    }
}

/** Lets pending promises settle, e.g. the lazily loaded theme tour or the preparation of a step. */
async function flushMicrotasks(): Promise<void> {
    for (let i = 0; i < 10; i++) {
        await Promise.resolve();
    }
}

function createUser(settings: Partial<User["settings"]>): User {
    return { settings } as unknown as User;
}

describe("TourService", () => {
    let service: TourService;
    let router: jasmine.SpyObj<Router>;
    let currentUrl: ReturnType<typeof signal<string>>;
    let userService: jasmine.SpyObj<UserService>;
    let currentEdge: { id: string } | null;
    let navigationTourConfig: NavigationTourConfig;
    let deviceService: jasmine.SpyObj<DeviceDetectorService>;
    let navigationPosition: ReturnType<typeof signal<"left" | "bottom" | "disabled" | null>>;

    const originalBackend = environment.backend;

    beforeEach(() => {
        // The service reads the shared environment object directly; the test environment connects to an edge
        setBackend("OpenEMS Backend");
        deviceService = jasmine.createSpyObj<DeviceDetectorService>("DeviceDetectorService", ["isDesktop"]);
        deviceService.isDesktop.and.returnValue(true);
        navigationPosition = signal<"left" | "bottom" | "disabled" | null>("left");
        const urlSerializer = new DefaultUrlSerializer();
        currentUrl = signal("/device/edge0/live?view=week");
        router = jasmine.createSpyObj<Router>("Router", ["navigateByUrl", "parseUrl"], {
            lastSuccessfulNavigation: computed(() => ({
                finalUrl: urlSerializer.parse(currentUrl()),
            })) as unknown as Router["lastSuccessfulNavigation"],
        });
        router.parseUrl.and.callFake((url) => urlSerializer.parse(url));
        router.navigateByUrl.and.returnValue(Promise.resolve(true));
        userService = jasmine.createSpyObj<UserService>("UserService", ["updateUserSettingsWithProperty"], {
            currentUser: signal<User | null>(createUser({})),
        });
        userService.updateUserSettingsWithProperty.and.callFake(async (key, value) => {
            userService.currentUser.set(createUser({ ...userService.currentUser()?.settings, [key]: value }));
        });
        currentEdge = null;
        themeTourSteps = [THEME_TOUR_STEP];
        navigationTourConfig = {
            avatar: null,
            load: jasmine.createSpy("load").and.resolveTo(ThemeNavigationTour),
        };
        TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot()],
            providers: [
                TourService,
                { provide: Router, useValue: router },
                { provide: Service, useValue: { currentEdge: () => currentEdge } },
                { provide: UserService, useValue: userService },
                { provide: DeviceDetectorService, useValue: deviceService },
                { provide: NavigationService, useValue: { position: navigationPosition } },
                { provide: NAVIGATION_TOUR, useFactory: () => navigationTourConfig },
            ],
        });
        service = TestBed.inject(TourService);
    });

    afterEach(() => {
        setBackend(originalBackend);
    });

    function setBackend(backend: DefaultTypes.Backend): void {
        Object.assign(environment, { backend });
    }

    it("does not navigate when the target route is active despite query parameters", async () => {
        currentEdge = { id: "edge0" };
        const step: TourStep = {
            id: "live",
            version: "2026.11.1",
            text: "Live",
            position: "center",
            route: "/live",
        };

        service.start([step]);
        await flushMicrotasks();

        expect(router.navigateByUrl).not.toHaveBeenCalled();
        expect(service.activeStep()).toBe(step);
    });

    it("cancels the tour when navigation to a step route fails", async () => {
        currentEdge = { id: "edge0" };
        currentUrl.set("/device/edge0/history");
        router.navigateByUrl.and.returnValue(Promise.resolve(false));
        const beforeShow = jasmine.createSpy("beforeShow");
        const step: TourStep = {
            id: "live",
            version: "2026.11.1",
            text: "Live",
            position: "center",
            route: "/live",
            beforeShow,
        };
        spyOn(console, "warn");

        service.start([step]);
        await flushMicrotasks();

        expect(router.navigateByUrl).toHaveBeenCalledOnceWith("/device/edge0/live");
        expect(beforeShow).not.toHaveBeenCalled();
        expect(service.isActive()).toBe(false);
        expect(service.activeStep()).toBeNull();
        expect(console.warn).toHaveBeenCalledOnceWith('Tour route "/device/edge0/live" could not be reached.');
        expect(userService.updateUserSettingsWithProperty).not.toHaveBeenCalled();
    });

    it("starts the desktop tour on desktops with the side menu navigation", async () => {
        service.startNavigationTour();
        await flushMicrotasks();

        expect(service.isActive()).toBe(true);
    });

    it("does not start the tour on tablets and smartphones yet", async () => {
        deviceService.isDesktop.and.returnValue(false);

        service.startNavigationTour();
        await flushMicrotasks();

        expect(service.isActive()).toBe(false);
    });

    it("reports the tour as available only on desktops with the side menu navigation", () => {
        expect(service.canStartNavigationTour()).toBe(true);

        navigationPosition.set("bottom");
        expect(service.canStartNavigationTour()).toBe(false);

        navigationPosition.set("left");
        deviceService.isDesktop.and.returnValue(false);
        expect(service.canStartNavigationTour()).toBe(false);
    });

    it("does not offer the tour on a local edge, which cannot store the tutorial state", async () => {
        setBackend("OpenEMS Edge");

        expect(service.canStartNavigationTour()).toBe(false);

        service.startNavigationTourIfNeeded();
        service.startNavigationTour();
        await flushMicrotasks();

        expect(service.isActive()).toBe(false);
    });

    it("does not start the tour without the side menu navigation", async () => {
        for (const position of ["bottom", "disabled", null] as const) {
            navigationPosition.set(position);
            service.startNavigationTour();
            await flushMicrotasks();

            expect(service.isActive()).withContext(String(position)).toBe(false);
        }
    });

    it("does not offer the tour when the theme provides none", async () => {
        const injector = Injector.create({
            providers: [{ provide: NAVIGATION_TOUR, useValue: null }],
            parent: TestBed.inject(Injector),
        });
        const serviceWithoutTour = runInInjectionContext(injector, () => new TourService());

        expect(serviceWithoutTour.canStartNavigationTour()).toBe(false);
        serviceWithoutTour.startNavigationTour();
        serviceWithoutTour.startNavigationTourIfNeeded();
        await flushMicrotasks();
        expect(serviceWithoutTour.isActive()).toBe(false);
    });

    it("starts the tour automatically when the user has not seen it yet", async () => {
        service.startNavigationTourIfNeeded();
        await flushMicrotasks();

        expect(service.isActive()).toBe(true);
    });

    it("does not start the tour automatically once the user completed or dismissed it", async () => {
        const states: TutorialState[] = [
            { state: "completed", uiVersion: "2026.11.1" },
            { state: "skipped", uiVersion: "2026.11.1" },
            { state: "completed", uiVersion: "2026.11.1-SNAPSHOT" },
            { state: "completed", uiVersion: "2026.12.0" },
        ];
        for (const state of states) {
            userService.currentUser.set(createUser({ [UserSettings.TUTORIAL_STATE]: state }));
            service.startNavigationTourIfNeeded();
            await flushMicrotasks();

            expect(service.isActive()).withContext(JSON.stringify(state)).toBe(false);
        }
    });

    it("starts the tour automatically again when a step is newer than the stored UI version", async () => {
        const states: TutorialState[] = [
            { state: "completed", uiVersion: "2026.10.2" },
            { state: "skipped", uiVersion: "2026.11.0" },
        ];
        for (const state of states) {
            userService.currentUser.set(createUser({ [UserSettings.TUTORIAL_STATE]: state }));
            // A new session, as ending the tour blocks the automatic start for the current one
            const sessionService = runInInjectionContext(TestBed.inject(Injector), () => new TourService());
            sessionService.startNavigationTourIfNeeded();
            await flushMicrotasks();

            expect(sessionService.isActive()).withContext(JSON.stringify(state)).toBe(true);
        }
    });

    it("compares the stored UI version with the highest step version", async () => {
        themeTourSteps = [THEME_TOUR_STEP, { ...THEME_TOUR_STEP, id: "new", version: "2026.12.0" }];
        userService.currentUser.set(
            createUser({ [UserSettings.TUTORIAL_STATE]: { state: "completed", uiVersion: "2026.11.1" } }),
        );

        service.startNavigationTourIfNeeded();
        await flushMicrotasks();

        expect(service.isActive()).toBe(true);
    });

    it("does not start the tour automatically before the user is loaded", async () => {
        userService.currentUser.set(null);
        service.startNavigationTourIfNeeded();
        await flushMicrotasks();

        expect(service.isActive()).toBe(false);
    });

    it("does not start the tour automatically again after it was closed in this session", async () => {
        service.startNavigationTourIfNeeded();
        await flushMicrotasks();
        service.skip();
        service.startNavigationTourIfNeeded();
        await flushMicrotasks();

        expect(service.isActive()).toBe(false);
    });

    it("does not start the tour automatically again in this session when storing its state failed", async () => {
        userService.updateUserSettingsWithProperty.and.rejectWith(new Error("Not supported by the edge"));
        spyOn(console, "error");

        service.startNavigationTourIfNeeded();
        await flushMicrotasks();
        service.next();
        await flushMicrotasks();
        service.startNavigationTourIfNeeded();
        await flushMicrotasks();

        expect(service.isActive()).toBe(false);
    });

    it("resolves the language of the handbook links", async () => {
        TestBed.inject(TranslateService).use("de");
        service.startNavigationTour();
        await flushMicrotasks();

        expect(service.activeStep()?.handbookUrl).toBe("https://docs.example.com/de/cockpit.html");
    });

    it("falls back to English handbook links for languages without a handbook", async () => {
        TestBed.inject(TranslateService).use("fr");
        service.startNavigationTour();
        await flushMicrotasks();

        expect(service.activeStep()?.handbookUrl).toBe("https://docs.example.com/en/cockpit.html");
    });

    it("loads the theme tour only once", async () => {
        service.startNavigationTour();
        await flushMicrotasks();
        service.cancel();
        service.startNavigationTour();
        await flushMicrotasks();

        expect(service.isActive()).toBe(true);
        expect(navigationTourConfig.load).toHaveBeenCalledTimes(1);
    });

    it("reports a failed tour download and retries on the next start", async () => {
        const error = new Error("Chunk download failed");
        const load = navigationTourConfig.load as jasmine.Spy;
        load.and.returnValues(Promise.reject(error), Promise.resolve(ThemeNavigationTour));
        spyOn(console, "error");

        service.startNavigationTour();
        await flushMicrotasks();
        expect(service.isActive()).toBe(false);
        expect(console.error).toHaveBeenCalledOnceWith("Failed to load the navigation tour.", error);

        service.startNavigationTour();
        await flushMicrotasks();
        expect(service.isActive()).toBe(true);
    });

    it("cancels the tour and reports errors from a step preparation callback", async () => {
        const error = new Error("Preparation failed");
        const step: TourStep = {
            id: "first",
            version: "2026.11.1",
            text: "First",
            position: "center",
            beforeShow: () => Promise.reject(error),
        };
        spyOn(console, "error");

        service.start([step]);
        // Lets the rejected step promise reach its error handler
        await new Promise((resolve) => setTimeout(resolve));

        expect(console.error).toHaveBeenCalledOnceWith("Failed to show tour step.", error);
        expect(service.isActive()).toBe(false);
        expect(service.activeStep()).toBeNull();
    });

    it("activates a step when its target is hidden inside an accordion the step will open", async () => {
        const accordionGroup = document.createElement("ion-accordion-group") as HTMLElement & {
            value: string | string[] | null;
        };
        accordionGroup.value = [];

        const accordion = document.createElement("ion-accordion");
        accordion.setAttribute("value", "closed-accordion");
        const target = document.createElement("div");
        target.dataset.tutorialId = TutorialTargetId.ENERGY_JOURNEY;
        target.style.display = "none";
        accordion.append(target);
        accordionGroup.append(accordion);
        document.body.append(accordionGroup);

        const step: TourStep = {
            id: "accordion-step",
            version: "2026.11.1",
            text: "Accordion step",
            targets: [{ id: TutorialTargetId.ENERGY_JOURNEY }],
            openParentAccordion: true,
        };

        try {
            expect(getActiveTourTarget(TutorialTargetId.ENERGY_JOURNEY, { allowClosedAccordion: true })).toBe(target);
            service.start([step]);
            await flushMicrotasks();

            expect(service.activeStep()).toBe(step);
        } finally {
            service.skip();
            accordionGroup.remove();
        }
    });

    it("waits for all targets of a step before activating it", async () => {
        const primaryTarget = createVisibleTarget(TutorialTargetId.HISTORY);
        const step: TourStep = {
            id: "history",
            version: "2026.11.1",
            text: "History",
            targets: [{ id: TutorialTargetId.HISTORY }, { id: TutorialTargetId.HISTORY_VIEW }],
        };
        jasmine.clock().install();

        try {
            service.start([step]);
            await advanceTime(100);
            expect(service.activeStep()).toBeNull();

            const secondaryTarget = createVisibleTarget(TutorialTargetId.HISTORY_VIEW);
            await advanceTime(20);
            expect(service.activeStep()).toBe(step);
            secondaryTarget.remove();
        } finally {
            jasmine.clock().uninstall();
            service.skip();
            primaryTarget.remove();
        }
    });

    it("activates a step with its primary target when an additional target does not appear", async () => {
        const primaryTarget = createVisibleTarget(TutorialTargetId.HISTORY);
        const step: TourStep = {
            id: "history",
            version: "2026.11.1",
            text: "History",
            targets: [{ id: TutorialTargetId.HISTORY }, { id: TutorialTargetId.HISTORY_VIEW }],
        };
        jasmine.clock().install();

        try {
            service.start([step]);
            await advanceTime(2000);

            expect(service.activeStep()).toBe(step);
        } finally {
            jasmine.clock().uninstall();
            service.skip();
            primaryTarget.remove();
        }
    });

    it("ends the tour without storing a state when a required target does not appear", async () => {
        spyOn(console, "warn");
        jasmine.clock().install();

        try {
            service.start([
                {
                    id: "history",
                    version: "2026.11.1",
                    text: "History",
                    targets: [{ id: TutorialTargetId.HISTORY }],
                },
            ]);
            await advanceTime(2000);

            expect(service.isActive()).toBe(false);
            expect(userService.updateUserSettingsWithProperty).not.toHaveBeenCalled();
            expect(console.warn).toHaveBeenCalledOnceWith(`Tour target "${TutorialTargetId.HISTORY}" not found.`);
        } finally {
            jasmine.clock().uninstall();
        }
    });

    it("navigates to the full device route before activating a step", async () => {
        currentEdge = { id: "edge0" };
        currentUrl.set("/device/edge0/history");
        const step: TourStep = {
            id: "live",
            version: "2026.11.1",
            text: "Live",
            position: "center",
            route: "/live",
        };

        service.start([step]);
        await flushMicrotasks();

        expect(router.navigateByUrl).toHaveBeenCalledOnceWith("/device/edge0/live");
        expect(service.activeStep()).toBe(step);
    });

    it("keeps the newest step active when an earlier step finishes late", async () => {
        let resolveFirstStep: () => void = () => {
            throw new Error("The first step preparation has not started.");
        };
        const firstStep: TourStep = {
            id: "first",
            version: "2026.11.1",
            text: "First",
            position: "center",
            beforeShow: () => new Promise<void>((resolve) => (resolveFirstStep = resolve)),
        };
        const secondStep: TourStep = {
            id: "second",
            version: "2026.11.1",
            text: "Second",
            position: "center",
        };

        service.start([firstStep, secondStep]);
        // Move on while the first step preparation is pending
        await flushMicrotasks();
        service.next();
        await flushMicrotasks();
        expect(service.activeStep()).toBe(secondStep);

        resolveFirstStep();
        await flushMicrotasks();

        expect(service.activeStep()).toBe(secondStep);
    });

    it("does not reactivate a step after cancellation", async () => {
        let resolveStep: () => void = () => {
            throw new Error("The step preparation has not started.");
        };
        const step: TourStep = {
            id: "first",
            version: "2026.11.1",
            text: "First",
            position: "center",
            beforeShow: () => new Promise<void>((resolve) => (resolveStep = resolve)),
        };

        service.start([step]);
        // Cancel while the step preparation is pending
        await flushMicrotasks();
        service.cancel();
        resolveStep();
        await flushMicrotasks();

        expect(service.isActive()).toBe(false);
        expect(service.activeStep()).toBeNull();
    });

    it("stores the completed tutorial state when the last step is finished", () => {
        service.start([{ id: "last", version: "2026.11.1", text: "Last", position: "center" }]);
        service.next();

        expect(userService.updateUserSettingsWithProperty).toHaveBeenCalledOnceWith(UserSettings.TUTORIAL_STATE, {
            state: "completed",
            uiVersion,
        });
        expect(service.isActive()).toBe(false);
    });

    it("stores the skipped tutorial state when the tour is cancelled", () => {
        service.start([{ id: "first", version: "2026.11.1", text: "First", position: "center" }]);
        service.cancel();

        expect(userService.updateUserSettingsWithProperty).toHaveBeenCalledOnceWith(UserSettings.TUTORIAL_STATE, {
            state: "skipped",
            uiVersion,
        });
        expect(service.isActive()).toBe(false);
    });

    it("does not store a tutorial state when no tour is running", () => {
        service.cancel();

        expect(userService.updateUserSettingsWithProperty).not.toHaveBeenCalled();
    });

    it("does not store a tutorial state when the tour is skipped", () => {
        service.skip();

        expect(userService.updateUserSettingsWithProperty).not.toHaveBeenCalled();
        expect(service.isActive()).toBe(false);
    });

    it("reports errors while storing the tutorial state", async () => {
        const error = new Error("Update failed");
        userService.updateUserSettingsWithProperty.and.rejectWith(error);
        spyOn(console, "error");

        service.start([{ id: "last", version: "2026.11.1", text: "Last", position: "center" }]);
        service.next();
        await flushMicrotasks();

        expect(console.error).toHaveBeenCalledOnceWith("Failed to store tutorial state.", error);
        expect(service.isActive()).toBe(false);
    });

    it("does not restart a tour that is already active", async () => {
        const firstStep: TourStep = { id: "first", version: "2026.11.1", text: "First", position: "center" };
        const secondStep: TourStep = { id: "second", version: "2026.11.1", text: "Second", position: "center" };
        const restartedStep: TourStep = {
            id: "restarted",
            version: "2026.11.1",
            text: "Restarted",
            position: "center",
        };

        service.start([firstStep, secondStep]);
        service.next();
        service.start([restartedStep]);
        await flushMicrotasks();

        expect(service.activeStep()).toBe(secondStep);
        expect(service.stepNumber()).toBe(2);
    });
});
