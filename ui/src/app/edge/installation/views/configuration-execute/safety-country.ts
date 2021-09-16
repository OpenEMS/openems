export enum SafetyCountry {
    Germany = "GERMANY",
    Austria = "AUSTRIA",
    Switzerland = "SWITZERLAND"
}

export namespace SafetyCountry {
    export function getSafetyCountry(countryValue: string) {
        switch (countryValue) {
            case "de":
                return SafetyCountry.Germany;
            case "at":
                return SafetyCountry.Austria;
            case "ch":
                return SafetyCountry.Switzerland;
        }
    }
}