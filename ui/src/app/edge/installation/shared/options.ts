import { TranslateService } from "@ngx-translate/core";
import { FeedInSetting } from "./enums";

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
        { label: north, value: north }
    ];
};

export const FEED_IN_POWER_FACTOR_OPTIONS = () => {
    return [
        // Leading
        { label: '0.80', value: FeedInSetting.Leading_0_80 },
        { label: '0.81', value: FeedInSetting.Leading_0_81 },
        { label: '0.82', value: FeedInSetting.Leading_0_82 },
        { label: '0.83', value: FeedInSetting.Leading_0_83 },
        { label: '0.84', value: FeedInSetting.Leading_0_84 },
        { label: '0.85', value: FeedInSetting.Leading_0_85 },
        { label: '0.86', value: FeedInSetting.Leading_0_86 },
        { label: '0.87', value: FeedInSetting.Leading_0_87 },
        { label: '0.88', value: FeedInSetting.Leading_0_88 },
        { label: '0.89', value: FeedInSetting.Leading_0_89 },
        { label: '0.90', value: FeedInSetting.Leading_0_90 },
        { label: '0.91', value: FeedInSetting.Leading_0_91 },
        { label: '0.92', value: FeedInSetting.Leading_0_92 },
        { label: '0.93', value: FeedInSetting.Leading_0_93 },
        { label: '0.94', value: FeedInSetting.Leading_0_94 },
        { label: '0.95', value: FeedInSetting.Leading_0_95 },
        { label: '0.96', value: FeedInSetting.Leading_0_96 },
        { label: '0.97', value: FeedInSetting.Leading_0_97 },
        { label: '0.98', value: FeedInSetting.Leading_0_98 },
        { label: '0.99', value: FeedInSetting.Leading_0_99 },
        { label: '1', value: FeedInSetting.Leading_1 },
        // Lagging
        { label: '-0.80', value: FeedInSetting.Lagging_0_80 },
        { label: '-0.81', value: FeedInSetting.Lagging_0_81 },
        { label: '-0.82', value: FeedInSetting.Lagging_0_82 },
        { label: '-0.83', value: FeedInSetting.Lagging_0_83 },
        { label: '-0.84', value: FeedInSetting.Lagging_0_84 },
        { label: '-0.85', value: FeedInSetting.Lagging_0_85 },
        { label: '-0.86', value: FeedInSetting.Lagging_0_86 },
        { label: '-0.87', value: FeedInSetting.Lagging_0_87 },
        { label: '-0.88', value: FeedInSetting.Lagging_0_88 },
        { label: '-0.89', value: FeedInSetting.Lagging_0_89 },
        { label: '-0.90', value: FeedInSetting.Lagging_0_90 },
        { label: '-0.91', value: FeedInSetting.Lagging_0_91 },
        { label: '-0.92', value: FeedInSetting.Lagging_0_92 },
        { label: '-0.93', value: FeedInSetting.Lagging_0_93 },
        { label: '-0.94', value: FeedInSetting.Lagging_0_94 },
        { label: '-0.95', value: FeedInSetting.Lagging_0_95 },
        { label: '-0.96', value: FeedInSetting.Lagging_0_96 },
        { label: '-0.97', value: FeedInSetting.Lagging_0_97 },
        { label: '-0.98', value: FeedInSetting.Lagging_0_98 },
        { label: '-0.99', value: FeedInSetting.Lagging_0_99 }
    ];
};