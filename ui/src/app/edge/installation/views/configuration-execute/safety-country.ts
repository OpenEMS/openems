export enum SafetyCountry {
    Germany = "GERMANY",
    Austria = "AUSTRIA"
}

export namespace SafetyCountry {
    export function getSafetyCountry(countryValue: string) {
        switch (countryValue) {
            case "ch": // TODO implement correctly if possible
            case "de":
                return SafetyCountry.Germany;
            case "at":
                return SafetyCountry.Austria;
        }
    }
}