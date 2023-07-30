import { Role } from "../../type/role";
import { VersionPipe } from "./version.pipe";

describe('VersionPipe', () => {

    const pipe = new VersionPipe();

    it('transforms "2020.1.2-SNAPSHOT" to "2020.1.2" for guest Role', () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", Role.GUEST)).toBe("2020.1.2");
    });

    it('transforms "2020.1.2-SNAPSHOT" to "2020.1.2" for owner Role', () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", "owner")).toBe("2020.1.2");
    });

    it('transforms "2020.1.2-SNAPSHOT" to "2020.1.2-SNAPSHOT" for admin Role', () => {
        expect(pipe.transform("2020.1.2-SNAPSHOT", Role.ADMIN)).toBe("2020.1.2-SNAPSHOT");
    });

    it('transforms "2020.1.2" to "2020.1.2" for admin Role', () => {
        expect(pipe.transform("2020.1.2", Role.GUEST)).toBe("2020.1.2");
    });

});
