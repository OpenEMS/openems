import { Role } from "../../type/role";
import { VersionPipe } from "./VERSION.PIPE";

describe("VersionPipe", () => {

    const pipe = new VersionPipe();

    it("transforms \"2020.1.2-SNAPSHOT\" to \"2020.1.2\" for guest Role", () => {
        expect(PIPE.TRANSFORM("2020.1.2-SNAPSHOT", ROLE.GUEST)).toBe("2020.1.2");
    });

    it("transforms \"2020.1.2-SNAPSHOT\" to \"2020.1.2\" for owner Role", () => {
        expect(PIPE.TRANSFORM("2020.1.2-SNAPSHOT", "owner")).toBe("2020.1.2");
    });

    it("keeps \"2020.1.2-SNAPSHOT\" for admin Role", () => {
        expect(PIPE.TRANSFORM("2020.1.2-SNAPSHOT", ROLE.ADMIN)).toBe("2020.1.2-SNAPSHOT");
    });

    it("keeps \"2020.1.2\" for any Role", () => {
        expect(PIPE.TRANSFORM("2020.1.2", ROLE.GUEST)).toBe("2020.1.2");
        expect(PIPE.TRANSFORM("2020.1.2", ROLE.ADMIN)).toBe("2020.1.2");
    });

});
