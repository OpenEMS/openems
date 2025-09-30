import { DecimalPipe, registerLocaleData } from "@angular/common";
import localDE from "@angular/common/locales/de";
import { Language } from "../../type/language";
import { UnitvaluePipe } from "./UNITVALUE.PIPE";

describe("UnitvaluePipe", () => {
    registerLocaleData(localDE);

    const pipe = new UnitvaluePipe(new DecimalPipe(LANGUAGE.EN.KEY));
    // TODO test for more i18n-locales
    // Note: "locale" value in DecimalPipe sets itself to default locale ('en-US') even though we specify our own locales.

    it("transforms \"1000 W\" to \"1.000 W\"", () => {
        expect(PIPE.TRANSFORM(1000, "W")).toBe("1.000" + "\u00A0" + "W");
    });

    it("transforms \"null W\" to \"- \"", () => {
        expect(PIPE.TRANSFORM(null, "W")).toBe("-" + "\u00A0");
    });
    it("transforms \"undefined W\" to \"- \"", () => {
        expect(PIPE.TRANSFORM(undefined, "W")).toBe("-" + "\u00A0");
    });

    it("transforms \"abc W\" to \"- \"", () => {
        expect(PIPE.TRANSFORM("abc", "W")).toBe("-" + "\u00A0");
    });

    it("transforms non number to \"-\"", () => {
        expect(PIPE.TRANSFORM(pipe, "W")).toBe("-" + "\u00A0");
    });

    it("transforms \"100 a\" to \"100 a\"", () => {
        expect(PIPE.TRANSFORM(100, "a")).toBe("100" + "\u00A0" + "a");
    });

});
