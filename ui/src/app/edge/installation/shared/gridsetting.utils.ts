import { GridSetting } from "./enums";

export namespace GridSettingUtils {

    export function getDisplayName(gridCode: GridSetting) {
        switch (gridCode) {
            case GridSetting.VDE_4105:
                return "VDE-AR-N 4105";
            case GridSetting.VDE_4110:
                return "VDE-AR-N 4110";
        }
    }
}
