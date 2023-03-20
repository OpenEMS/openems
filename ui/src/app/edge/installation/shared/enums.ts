export enum FeedInSetting {
    QuEnableCurve = "QU_ENABLE_CURVE",
    PuEnableCurve = "PU_ENABLE_CURVE",
    FixedPowerFactor = "FIXED_POWER_FACTOR",
    // Lagging Power Factor
    Lagging_0_80 = "LAGGING_0_80",
    Lagging_0_81 = "LAGGING_0_81",
    Lagging_0_82 = "LAGGING_0_82",
    Lagging_0_83 = "LAGGING_0_83",
    Lagging_0_84 = "LAGGING_0_84",
    Lagging_0_85 = "LAGGING_0_85",
    Lagging_0_86 = "LAGGING_0_86",
    Lagging_0_87 = "LAGGING_0_87",
    Lagging_0_88 = "LAGGING_0_88",
    Lagging_0_89 = "LAGGING_0_89",
    Lagging_0_90 = "LAGGING_0_90",
    Lagging_0_91 = "LAGGING_0_91",
    Lagging_0_92 = "LAGGING_0_92",
    Lagging_0_93 = "LAGGING_0_93",
    Lagging_0_94 = "LAGGING_0_94",
    Lagging_0_95 = "LAGGING_0_95",
    Lagging_0_96 = "LAGGING_0_96",
    Lagging_0_97 = "LAGGING_0_97",
    Lagging_0_98 = "LAGGING_0_98",
    Lagging_0_99 = "LAGGING_0_99",
    // Leading Power Factor
    Leading_0_80 = "LEADING_0_80",
    Leading_0_81 = "LEADING_0_81",
    Leading_0_82 = "LEADING_0_82",
    Leading_0_83 = "LEADING_0_83",
    Leading_0_84 = "LEADING_0_84",
    Leading_0_85 = "LEADING_0_85",
    Leading_0_86 = "LEADING_0_86",
    Leading_0_87 = "LEADING_0_87",
    Leading_0_88 = "LEADING_0_88",
    Leading_0_89 = "LEADING_0_89",
    Leading_0_90 = "LEADING_0_90",
    Leading_0_91 = "LEADING_0_91",
    Leading_0_92 = "LEADING_0_92",
    Leading_0_93 = "LEADING_0_93",
    Leading_0_94 = "LEADING_0_94",
    Leading_0_95 = "LEADING_0_95",
    Leading_0_96 = "LEADING_0_96",
    Leading_0_97 = "LEADING_0_97",
    Leading_0_98 = "LEADING_0_98",
    Leading_0_99 = "LEADING_0_99",
    Leading_1 = "LEADING_1",
    Undefined = "UNDEFINED"
}

export enum FeedInType {
    DYNAMIC_LIMITATION,
    EXTERNAL_LIMITATION
}

export enum WebLinks {
    GTC_LINK,
    WARRANTY_LINK_HOME,
    WARRANTY_LINK_COMMERCIAL
}

export namespace WebLinks {
    export function getLink(link: WebLinks): string {
        switch (link) {
            case WebLinks.GTC_LINK:
                return 'https://fenecon.de/allgemeine-lieferungs-und-zahlungsbedingungen/';
            case WebLinks.WARRANTY_LINK_HOME:
                return 'https://fenecon.de/wp-content/uploads/2022/06/V2021.11_DE_Garantiebedingungen_FENECON_Home.pdf';
            case WebLinks.WARRANTY_LINK_COMMERCIAL:
                return 'https://fenecon.de/wp-content/uploads/2022/07/V2022.03_DE_Garantiebedingungen_FENECON_Commercial_30_50.pdf';
        }
    }
}
