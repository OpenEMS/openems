import { getActiveTourTarget, getBoundingRect, getOverlapArea, getPaddedHighlightRects, getTourVersion, selectTooltipPlacement, } from "./tour.utils";

describe("getOverlapArea", () => {
    it("returns the shared area of overlapping rectangles", () => {
        expect(
            getOverlapArea({ top: 0, left: 0, width: 10, height: 10 }, { top: 5, left: 5, width: 10, height: 10 }),
        ).toBe(25);
    });

    it("returns 0 for rectangles that only touch", () => {
        expect(
            getOverlapArea({ top: 0, left: 0, width: 10, height: 10 }, { top: 0, left: 10, width: 10, height: 10 }),
        ).toBe(0);
    });
});

describe("getBoundingRect", () => {
    it("encloses all rectangles", () => {
        const rect = getBoundingRect([new DOMRect(10, 100, 50, 20), new DOMRect(200, 20, 100, 40)]);

        expect([rect.left, rect.top, rect.right, rect.bottom]).toEqual([10, 20, 300, 120]);
    });
});

describe("getPaddedHighlightRects", () => {
    it("pads a single target on every side", () => {
        expect(getPaddedHighlightRects([new DOMRect(10, 20, 100, 50)], 4)).toEqual([
            { top: 16, left: 6, width: 108, height: 58 },
        ]);
    });

    it("splits touching horizontal neighbours in the middle instead of overlapping", () => {
        const [menu, content] = getPaddedHighlightRects(
            [new DOMRect(0, 100, 380, 100), new DOMRect(380, 0, 500, 500)],
            4,
        );

        expect(menu.left + menu.width).toBe(378);
        expect(content.left).toBe(382);
    });

    it("splits vertical neighbours regardless of their order", () => {
        const [bottom, top] = getPaddedHighlightRects([new DOMRect(0, 102, 100, 50), new DOMRect(0, 0, 100, 100)], 4);

        expect(top.top + top.height).toBe(99);
        expect(bottom.top).toBe(103);
    });

    it("keeps the full padding for targets that are far enough apart", () => {
        const [first, second] = getPaddedHighlightRects(
            [new DOMRect(0, 0, 100, 100), new DOMRect(200, 0, 100, 100)],
            4,
        );

        expect(first.width).toBe(108);
        expect(second.left).toBe(196);
    });
});

describe("getActiveTourTarget", () => {
    let container: HTMLDivElement;

    beforeEach(() => {
        container = document.createElement("div");
        document.body.append(container);
    });

    afterEach(() => {
        container.remove();
    });

    it("skips hidden matching elements and returns a visible one", () => {
        const hidden = document.createElement("div");
        hidden.dataset.tutorialId = "target";
        hidden.style.display = "none";

        const visible = document.createElement("div");
        visible.dataset.tutorialId = "target";
        visible.style.width = "20px";
        visible.style.height = "20px";
        container.append(hidden, visible);

        expect(getActiveTourTarget("target")).toBe(visible);
    });

    it("skips targets with hidden visibility", () => {
        const target = document.createElement("div");
        target.dataset.tutorialId = "target";
        target.style.visibility = "hidden";
        container.append(target);

        expect(getActiveTourTarget("target")).toBeNull();
    });

    it("skips targets without a visible layout rectangle", () => {
        const target = document.createElement("div");
        target.dataset.tutorialId = "target";
        target.style.width = "0";
        target.style.height = "0";
        container.append(target);

        expect(getActiveTourTarget("target")).toBeNull();
    });

    it("checks visibility on the selected child target", () => {
        const parent = document.createElement("div");
        parent.dataset.tutorialId = "target";
        const child = document.createElement("div");
        child.className = "child";
        child.style.display = "none";
        parent.append(child);
        container.append(parent);

        expect(getActiveTourTarget("target", { childSelector: ".child" })).toBeNull();
    });

    it("resolves hidden targets inside a closed accordion only when requested", () => {
        const accordionGroup = document.createElement("ion-accordion-group") as HTMLElement & {
            value: string | string[] | null;
        };
        accordionGroup.value = [];

        const accordion = document.createElement("ion-accordion");
        accordion.setAttribute("value", "closed-accordion");
        const target = document.createElement("div");
        target.dataset.tutorialId = "target";
        target.style.display = "none";
        accordion.append(target);
        accordionGroup.append(accordion);
        container.append(accordionGroup);

        expect(getActiveTourTarget("target")).toBeNull();
        expect(getActiveTourTarget("target", { allowClosedAccordion: true })).toBe(target);
    });
});

describe("getTourVersion", () => {
    it("returns the highest step version", () => {
        expect(
            getTourVersion([
                { id: "first", version: "2026.11.1" },
                { id: "second", version: "2027.2.0" },
                { id: "third", version: "2026.12.0" },
            ]),
        ).toBe("2027.2.0");
    });

    it("returns null for a tour without steps", () => {
        expect(getTourVersion([])).toBeNull();
    });
});

describe("selectTooltipPlacement", () => {
    const highlight = { top: 0, left: 0, width: 100, height: 100 };
    const navigation = { top: 0, left: 200, width: 50, height: 400 };

    it("returns the first placement covering neither a highlight nor the navigation", () => {
        const covering = { top: 50, left: 50, width: 20, height: 20 };
        const free = { top: 0, left: 110, width: 20, height: 20 };

        expect(selectTooltipPlacement([covering, free], [highlight], navigation)).toBe(free);
    });

    it("prefers covering highlights to covering the navigation", () => {
        const onNavigation = { top: 0, left: 210, width: 20, height: 20 };
        const covering = { top: 50, left: 50, width: 20, height: 20 };

        expect(selectTooltipPlacement([onNavigation, covering], [highlight], navigation)).toBe(covering);
    });

    it("returns the placement covering the least highlighted area", () => {
        const coveringHalf = { top: 0, left: 0, width: 50, height: 100 };
        const coveringQuarter = { top: 0, left: 0, width: 50, height: 50 };

        expect(selectTooltipPlacement([coveringHalf, coveringQuarter], [highlight], null)).toBe(coveringQuarter);
    });

    it("returns the placement covering the least navigation if every placement covers it", () => {
        const coveringMuch = { top: 0, left: 200, width: 40, height: 40 };
        const coveringLittle = { top: 0, left: 240, width: 40, height: 40 };

        expect(selectTooltipPlacement([coveringMuch, coveringLittle], [highlight], navigation)).toBe(coveringLittle);
    });

    it("returns null without placements", () => {
        expect(selectTooltipPlacement([], [highlight], navigation)).toBeNull();
    });
});
