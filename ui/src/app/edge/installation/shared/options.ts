import { TranslateService } from "@ngx-translate/core";

export const DIRECTIONS_OPTIONS = (translate: TranslateService) => {
    const south = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.SOUTH');
    const southWest = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.SOUTH_WEST');
    const west = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.WEST');
    const southEast = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.SOUTH_EAST');
    const east = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.EAST');
    const northWest = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.NORTH_WEST');
    const northEast = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.NORTH_EAST');
    const north = translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.DIRECTIONS.NORTH');
    return [
        { label: south, value: south },
        { label: southWest, value: southWest },
        { label: west, value: west },
        { label: southEast, value: southEast },
        { label: east, value: east },
        { label: northWest, value: northWest },
        { label: northEast, value: northEast },
        { label: north, value: north },
    ];
};