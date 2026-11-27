import { signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { AngularDelegate } from "@ionic/angular";
import { Theme } from "src/app/edge/history/shared";
import { User } from "src/app/shared/jsonrpc/shared";
import { UserService } from "src/app/shared/service/user.service";
import { Service } from "src/app/shared/shared";
import { FilterComponent, Filter } from "./filter.component";

describe("FilterComponent", () => {
    let component: FilterComponent;

    const userServiceSpyObj = jasmine.createSpyObj<UserService>("UserService", ["currentUser"], {
        currentUser: signal(new User("", "", "admin", "", true, { theme: Theme.LIGHT })),
    });
    beforeEach(() => {
        userServiceSpyObj.currentUser.set(new User("", "", "admin", "", true, { theme: Theme.LIGHT }));
        TestBed.configureTestingModule({
            imports: [FilterComponent],
            providers: [
                { provide: Service, useValue: {} },
                { provide: AngularDelegate, useValue: {} },
                { provide: UserService, useValue: userServiceSpyObj },
            ],
        });

        const fixture = TestBed.createComponent(FilterComponent);
        component = fixture.componentInstance;
        localStorage.clear();
    });

    afterEach(() => {
        localStorage.clear();
    });

    it("should create", () => {
        expect(component).toBeTruthy();
    });

    it("should not emit when the filters are unchanged", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "status",
            options: [{ name: "Active", option: { value: "active", default: true } }],
        };
        const emitSpy = spyOn(component.setSearchParams, "emit");

        component.filters = [filter];
        expect(emitSpy).toHaveBeenCalledTimes(1);

        component.filters = structuredClone([filter]);
        expect(emitSpy).toHaveBeenCalledTimes(1);
    });

    it("should not emit for empty filters", () => {
        const emitSpy = spyOn(component.setSearchParams, "emit");

        component.filters = [];
        component.filters = [];

        expect(emitSpy).not.toHaveBeenCalled();
    });

    it("should not emit filters for a guest", () => {
        userServiceSpyObj.currentUser.set(new User("", "", "guest", "", true, {}));
        const emitSpy = spyOn(component.setSearchParams, "emit");

        component.filters = [];
        component.filters = [];

        expect(emitSpy).not.toHaveBeenCalled();
    });

    it("should set default filter values based on option.default", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "status",
            options: [
                { name: "Active", option: { value: "active", default: true } },
                { name: "Inactive", option: { value: "inactive", default: false } },
            ],
        };

        component.filters = [filter];

        const searchParams = (component as any).searchParams as Map<string, unknown>;
        expect(searchParams.get("status")).toEqual(["active"]);
    });

    it("should not apply defaults when no option is marked default", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "type",
            options: [
                { name: "A", option: { value: "a" } },
                { name: "B", option: { value: "b" } },
            ],
        };

        component.filters = [filter];

        const searchParams = (component as any).searchParams as Map<string, unknown>;
        expect(searchParams.has("type")).toBeFalse();
    });

    it("should skip null filters when setting filters", () => {
        component.filters = [null];
        expect((component as any).allFilters).toEqual([]);
    });

    it("should update searchParams and persist selection on searchOnChange", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "status",
            options: [{ name: "Active", option: { value: "active", id: "id-1" } }],
        };

        component.filters = [filter];

        const emitSpy = spyOn(component.setSearchParams, "emit");
        const fakeEvent = { target: { value: "id-1" } } as unknown as Event;

        component.searchOnChange(fakeEvent, filter);

        const searchParams = (component as any).searchParams as Map<string, unknown>;
        expect(searchParams.get("status")).toBeUndefined();
        expect(emitSpy).toHaveBeenCalledWith(searchParams);
        expect(JSON.parse(localStorage.getItem("status")!)).toEqual([]);
    });

    it("should remove searchParam entry when selection is cleared", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "status",
            options: [{ name: "Active", option: { value: "active", id: "id-1" } }],
        };

        component.filters = [filter];
        (component as any).searchParams.set("status", ["active"]);

        const fakeEvent = { target: { value: [] } } as unknown as Event;
        component.searchOnChange(fakeEvent, filter);

        const searchParams = (component as any).searchParams as Map<string, unknown>;
        expect(searchParams.has("status")).toBeFalse();
    });

    it("should ignore searchOnChange when event value is null", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "status",
            options: [{ name: "Active", option: { value: "active", id: "id-1" } }],
        };

        component.filters = [filter];
        const emitSpy = spyOn(component.setSearchParams, "emit");

        const fakeEvent = { target: { value: null } } as unknown as Event;
        component.searchOnChange(fakeEvent, filter);

        expect(emitSpy).not.toHaveBeenCalled();
    });

    it("getIdByValue should return matching option ids for given values", () => {
        const filter: Filter<string> = {
            multiple: false,
            placeholder: "placeholder",
            category: "status",
            options: [
                { name: "Active", option: { value: "active", id: "id-1" } },
                { name: "Inactive", option: { value: "inactive", id: "id-2" } },
            ],
        };

        const ids = FilterComponent.getIdByValue(filter, ["active"]);
        expect(ids).toEqual(["id-1"]);
    });
});
