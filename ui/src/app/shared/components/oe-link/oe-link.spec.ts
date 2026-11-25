import { ComponentFixture, TestBed } from "@angular/core/testing";
import { OeLinkComponent } from "./oe-link";

describe("OeLinkComponent", () => {
    let fixture: ComponentFixture<OeLinkComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [OeLinkComponent],
        }).compileComponents();
    });

    it("renders an external link with safe target attributes", () => {
        fixture = TestBed.createComponent(OeLinkComponent);
        fixture.componentRef.setInput("link", { text: "Documentation", href: "/docs" });
        fixture.detectChanges();

        const anchor = fixture.nativeElement.querySelector("a") as HTMLAnchorElement;

        expect(anchor.textContent).toBe("Documentation");
        expect(anchor.getAttribute("href")).toBe("/docs");
        expect(anchor.getAttribute("target")).toBe("_blank");
        expect(anchor.getAttribute("rel")).toBe("noopener noreferrer");
    });

    it("invokes the callback for an action link", () => {
        const callback = jasmine.createSpy("callback");
        fixture = TestBed.createComponent(OeLinkComponent);
        fixture.componentRef.setInput("link", { text: "Run action", callback });
        fixture.detectChanges();

        const button = fixture.nativeElement.querySelector("button") as HTMLButtonElement;
        button.click();

        expect(callback).toHaveBeenCalledTimes(1);
    });
});
