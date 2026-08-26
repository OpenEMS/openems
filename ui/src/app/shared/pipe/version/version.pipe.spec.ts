import { Role } from "../../type/role";
import { VersionPipe } from "./version.pipe";

describe("VersionPipe", () => {

    const pipe = new VersionPipe();

    it("transforms \"2020.1.2-SNAPSHOT\" to \"2020.1.2\" for guest Role", () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", Role.GUEST)).toBe("2020.1.2");
    });

    it("transforms \"2020.1.2-SNAPSHOT\" to \"2020.1.2\" for owner Role", () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", "owner")).toBe("2020.1.2");
    });

    it("transforms \"2020.1.2-SNAPSHOT\" to \"2020.1.2\" for installer Role", () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", Role.INSTALLER)).toBe("2020.1.2");
    });

    it("keeps \"2020.1.2-SNAPSHOT\" for admin Role", () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", Role.ADMIN)).toBe("2020.1.2-SNAPSHOT");
    });

    it("keeps \"2020.1.2\" for any Role", () => {
        expect(pipe.transform("2020.1.2", Role.GUEST)).toBe("2020.1.2");
        expect(pipe.transform("2020.1.2", Role.ADMIN)).toBe("2020.1.2");
    });

    it("transforms \"2026.2.1-MULTI-HYPHEN\" to \"2026.2.1\"", () => {
        expect(pipe.transform("2026.2.1-MULTI-HYPHEN", Role.GUEST)).toBe("2026.2.1");
        expect(pipe.transform("2026.2.1-MULTI-HYPHEN", Role.INSTALLER)).toBe("2026.2.1");
    });

});
