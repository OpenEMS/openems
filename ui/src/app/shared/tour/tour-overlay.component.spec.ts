import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { TourOverlayComponent } from "./tour-overlay.component";
import { TourStep } from "./tour-step";
import { TourService } from "./tour.service";
import { TutorialTargetId } from "./tutorial-target-id";

describe("TourOverlayComponent", () => {
    let fixture: ComponentFixture<TourOverlayComponent>;
    let activeStep: ReturnType<typeof signal<TourStep | null>>;
    let isActive: ReturnType<typeof signal<boolean>>;
    let accordionGroup: HTMLElement & { value: string | string[] | null };

    beforeEach(async () => {
        activeStep = signal<TourStep | null>(null);
        isActive = signal(true);
        const tourServiceMock = {
            activeStep,
            isActive,
            stepNumber: signal(1),
            stepCount: signal(1),
            isFirstStep: signal(true),
            isLastStep: signal(true),
            cancel: jasmine.createSpy("cancel"),
            next: jasmine.createSpy("next"),
            previous: jasmine.createSpy("previous"),
            skip: jasmine.createSpy("skip"),
        };

        await TestBed.configureTestingModule({
            imports: [TourOverlayComponent, TranslateModule.forRoot()],
            providers: [{ provide: TourService, useValue: tourServiceMock }],
        }).compileComponents();

        fixture = TestBed.createComponent(TourOverlayComponent);

        accordionGroup = document.createElement("ion-accordion-group") as HTMLElement & {
            value: string | string[] | null;
        };
        accordionGroup.value = ["user-open"];

        const accordion = document.createElement("ion-accordion");
        accordion.setAttribute("value", "tour-open");
        const target = document.createElement("div");
        target.dataset.tutorialId = TutorialTargetId.ENERGY_MONITOR;
        target.style.display = "none";
        target.style.width = "20px";
        target.style.height = "20px";
        accordion.append(target);
        accordionGroup.append(accordion);
        document.body.append(accordionGroup);
    });

    afterEach(() => {
        fixture.destroy();
        accordionGroup.remove();
    });

    it("restores accordion values that were open before the tour", () => {
        const step: TourStep = {
            id: "accordion",
            version: "2026.11.1",
            text: "Accordion",
            targets: [{ id: TutorialTargetId.ENERGY_MONITOR }],
            position: "auto",
            closeAllAccordions: true,
            openParentAccordion: true,
        };

        fixture.detectChanges();
        activeStep.set(step);
        fixture.detectChanges();
        expect(accordionGroup.value).toEqual(["tour-open"]);

        activeStep.set(null);
        fixture.detectChanges();

        expect(accordionGroup.value).toEqual(["user-open"]);
    });

    it("closes accordions the tour opened in groups rendered after the tour started", () => {
        accordionGroup.remove();
        fixture.detectChanges();
        activeStep.set({
            id: "intro",
            version: "2026.11.1",
            text: "Intro",
            position: "center",
        });
        fixture.detectChanges();

        // Rendered later, e.g. by the route of the next step
        accordionGroup.value = [];
        document.body.append(accordionGroup);
        activeStep.set({
            id: "accordion",
            version: "2026.11.1",
            text: "Accordion",
            targets: [{ id: TutorialTargetId.ENERGY_MONITOR }],
            position: "auto",
            openParentAccordion: true,
        });
        fixture.detectChanges();
        expect(accordionGroup.value).toEqual(["tour-open"]);

        activeStep.set(null);
        fixture.detectChanges();

        expect(accordionGroup.value).toEqual([]);
    });

    it("does not cover the navigation, even if every placement covers a target", async () => {
        const createFixedElement = (id: TutorialTargetId, style: Partial<CSSStyleDeclaration>): HTMLElement => {
            const element = document.createElement("div");
            element.dataset.tutorialId = id;
            Object.assign(element.style, { position: "fixed", ...style });
            return element;
        };
        const navigation = createFixedElement(TutorialTargetId.NAVIGATION, {
            left: "0",
            top: "0",
            width: "20vw",
            height: "100vh",
        });
        navigation.append(
            createFixedElement(TutorialTargetId.APP_CENTER, { left: "0", top: "40vh", width: "20vw", height: "40px" }),
        );
        const page = createFixedElement(TutorialTargetId.ENERGY_JOURNEY_APP_CENTER, {
            left: "22vw",
            top: "0",
            width: "78vw",
            height: "100vh",
        });
        document.body.append(navigation, page);

        try {
            fixture.detectChanges();
            activeStep.set({
                id: "appcenter",
                version: "2026.11.1",
                text: "App Center",
                targets: [{ id: TutorialTargetId.APP_CENTER }, { id: TutorialTargetId.ENERGY_JOURNEY_APP_CENTER }],
                position: "auto",
            });
            fixture.detectChanges();
            for (let i = 0; i < 3; i++) {
                await new Promise((resolve) => requestAnimationFrame(resolve));
            }
            fixture.detectChanges();

            const tooltip: HTMLElement = fixture.nativeElement.querySelector(".tour-tooltip");
            // Read the target coordinates, as the tooltip is still moving there
            expect(tooltip.style.left).not.toBe("");
            expect(parseFloat(tooltip.style.left)).toBeGreaterThanOrEqual(navigation.getBoundingClientRect().right);
        } finally {
            navigation.remove();
            page.remove();
        }
    });

    it("blocks the page while the tour is active", () => {
        const host: HTMLElement = fixture.nativeElement;
        const parent = document.createElement("div");
        const page = document.createElement("div");
        const alreadyInert = document.createElement("div");
        alreadyInert.inert = true;
        parent.append(page, alreadyInert);
        host.replaceWith(parent);
        parent.append(host);

        isActive.set(false);
        fixture.detectChanges();
        expect(page.inert).toBe(false);
        expect(host.querySelector(".tour-interaction-blocker")).toBeNull();

        isActive.set(true);
        fixture.detectChanges();
        expect(page.inert).toBe(true);
        expect(host.inert).toBe(false);
        expect(host.querySelector(".tour-interaction-blocker")).not.toBeNull();

        isActive.set(false);
        fixture.detectChanges();
        expect(page.inert).toBe(false);
        expect(alreadyInert.inert).toBe(true);

        parent.remove();
    });

    it("applies accordion closing options on centered steps", () => {
        const step: TourStep = {
            id: "intro",
            version: "2026.11.1",
            text: "Intro",
            position: "center",
            closeAllAccordions: true,
        };

        fixture.detectChanges();
        activeStep.set(step);
        fixture.detectChanges();
        expect(accordionGroup.value).toEqual([]);

        activeStep.set(null);
        fixture.detectChanges();

        expect(accordionGroup.value).toEqual(["user-open"]);
    });

    it("renders highlighted parts of the step texts as bold", () => {
        const step: TourStep = {
            id: "formatted",
            version: "2026.11.1",
            contentBlocks: [
                {
                    text: "In the <strong>History</strong> section",
                    items: [{ text: "<strong>SoC</strong>", color: "red" }],
                },
            ],
            position: "center",
        };

        fixture.detectChanges();
        activeStep.set(step);
        fixture.detectChanges();

        const host: HTMLElement = fixture.nativeElement;
        const boldTexts = Array.from(host.querySelectorAll(".tour-text strong")).map((element) => element.textContent);
        expect(boldTexts).toEqual(["History", "SoC"]);
    });

    it("renders highlighted parts of a plain step text as bold", () => {
        const step: TourStep = {
            id: "plain",
            version: "2026.11.1",
            text: "Go to <strong>Info</strong>",
            position: "center",
        };

        fixture.detectChanges();
        activeStep.set(step);
        fixture.detectChanges();

        const host: HTMLElement = fixture.nativeElement;
        expect(host.querySelector(".tour-text strong")?.textContent).toBe("Info");
        expect(host.querySelector(".tour-text")?.textContent).not.toContain("<strong>");
    });
});
