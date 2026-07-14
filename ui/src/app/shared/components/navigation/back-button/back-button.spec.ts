import { signal, WritableSignal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { RouterModule } from "@angular/router";
import { CommonUiModule } from "../../../common-ui.module";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";
import { NavigationBackButtonComponent } from "./back-button";

describe("NavigationBackButtonComponent", () => {
    let fixture: ComponentFixture<NavigationBackButtonComponent>;
    let component: NavigationBackButtonComponent;
    let currentNodeSignal: WritableSignal<NavigationTree | null>;

    const mockParent: NavigationTree = new NavigationTree(
        "parent",
        { baseString: "/parent" },
        { name: "icon" },
        "Parent",
        "label",
        [],
        null,
    );

    const hiddenNode: NavigationTree = new NavigationTree(
        "hiddenChild",
        { baseString: "/parent/hidden" },
        { name: "icon" },
        "Hidden child",
        "label",
        [],
        mockParent,
        { showOrder: "HIDE" },
    );

    const visibleNode: NavigationTree = new NavigationTree(
        "child",
        { baseString: "/parent/child" },
        { name: "icon" },
        "Child",
        "label",
        [],
        mockParent,
    );

    beforeEach(async () => {
        currentNodeSignal = signal<NavigationTree | null>(null);

        await TestBed.configureTestingModule({
            imports: [NavigationBackButtonComponent, CommonUiModule, RouterModule.forRoot([])],
            providers: [
                {
                    provide: NavigationService,
                    useValue: {
                        currentNode: currentNodeSignal,
                        position: () => "left",
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(NavigationBackButtonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });

    it("should set parentNode to null when currentNode is null", () => {
        currentNodeSignal.set(null);
        fixture.detectChanges();

        expect(component["parentNode"]).toBeNull();
        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector("ion-item")).toBeNull();
    });

    it("should set parentNode to null when currentNode showOrder is not HIDE", () => {
        currentNodeSignal.set(visibleNode);
        fixture.detectChanges();

        expect(component["parentNode"]).toBeNull();
        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector("ion-item")).toBeNull();
    });

    it("should set parentNode to parent when currentNode showOrder is HIDE", () => {
        currentNodeSignal.set(hiddenNode);
        fixture.detectChanges();

        expect(component["parentNode"]).toBe(mockParent);
        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector("ion-item")).toBeTruthy();
    });

    it("should not render ion-item when parentNode is null", () => {
        currentNodeSignal.set(null);
        fixture.detectChanges();

        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector("ion-item")).toBeNull();
    });

    it("should render ion-item when parentNode is set", () => {
        currentNodeSignal.set(hiddenNode);
        fixture.detectChanges();

        const el: HTMLElement = fixture.nativeElement;
        expect(el.querySelector("ion-item")).toBeTruthy();
    });
});
