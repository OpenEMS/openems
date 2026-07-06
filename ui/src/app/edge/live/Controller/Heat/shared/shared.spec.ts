import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { TestContext, TestingUtils, } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { SharedControllerHeat } from "./shared";

describe("SharedControllerHeat", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        testContext.translate.setTranslation(
            "en",
            {
                GENERAL: {
                    HISTORY: "History",
                },
                HEAT: {
                    SCHEDULE: {
                        SCHEDULE: "Schedule",
                    },
                },
                JS_SCHEDULE: {
                    ADD_TASK: "Add task",
                    EDIT_TASK: "Edit task",
                },
                MENU: {
                    SETTINGS: "Settings",
                },
            },
            true,
        );
        testContext.translate.use("en");
    });

    it("#getNavigationTree() includes schedule and settings for writable Askoma", () => {
        const component = new EdgeConfig.Component(
            "heat0",
            "ASKOMA",
            true,
            false,
            "Heat.Askoma",
            {},
        );

        const navigationTree = getNavigationTree(component, true);

        expect(navigationTree.children.map((child) => child.id)).toEqual([
            "forecast",
            "history",
            "schedule",
            "settings",
        ]);
    });

    it("#getNavigationTree() hides schedule and settings for read-only Askoma", () => {
        const component = new EdgeConfig.Component(
            "heat0",
            "ASKOMA",
            true,
            false,
            "Heat.Askoma",
            { readOnly: true },
        );

        const navigationTree = getNavigationTree(component, true);

        expect(navigationTree.children.map((child) => child.id)).toEqual([
            "history",
        ]);
    });

    it("#getNavigationTree() includes settings and schedule for MyPV Heat", () => {
        const component = new EdgeConfig.Component(
            "heat1",
            "Heat",
            true,
            false,
            "Heat.MyPv",
            {},
        );

        const navigationTree = getNavigationTree(component, false);
        expect(navigationTree.children.length).toEqual(3);
        expect(navigationTree.children.map((child) => child.id)).toEqual([
            "history",
            "schedule",
            "settings",
        ]);
    });

    function getNavigationTree(
        component: EdgeConfig.Component,
        isAskoma: boolean,
    ): NavigationTree {
        return new NavigationTree(
            ...SharedControllerHeat.getNavigationTree(
                testContext.translate,
                component,
                isAskoma,
            ),
        );
    }
});
