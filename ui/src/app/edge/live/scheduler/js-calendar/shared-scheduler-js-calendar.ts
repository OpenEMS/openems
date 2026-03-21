import { TranslateService } from "@ngx-translate/core";
import { ValidationResult } from "json-schema";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { JsCalendar } from "src/app/shared/components/schedule/js-calendar-task";
import { OneTask } from "src/app/shared/jsonrpc/response/getOneTasksResponse";
import { EdgeConfig, Service } from "src/app/shared/shared";
import { WidgetFactory } from "src/app/shared/type/widget";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";

export namespace SharedSchedulerJsCalendar {

    export function getNavigationTree(translate: TranslateService, componentId: EdgeConfig.Component["id"]): ConstructorParameters<typeof NavigationTree> | null {
        return new NavigationTree(componentId + "/scheduler-js-calendar", { baseString: componentId + "/scheduler-js-calendar" }, { name: "battery-full-outline", color: "medium" }, "Serieller Multi Use", "label", [
            new NavigationTree("schedule", { baseString: "schedule" }, { name: "calendar-outline", color: "warning" }, translate.instant("EDGE.INDEX.WIDGETS.EVSE.SCHEDULE.SCHEDULE"), "label", [], null),
        ], null).toConstructorParams();
    }

    /**
     * Gets the controller options.
     *
     * @param service the service
     * @returns a list of available values with labels
     */
    export function getControllerOptions(service: Service): { value: string, label: string }[] {
        const edge = service.currentEdge();
        const config = edge.getConfigSignal()();
        const factories = Object.values(config?.factories ?? {})?.flat()?.filter(el => el.componentIds?.length > 0) ?? [];
        const controllerOptions: { value: string, label: string }[] = [];

        for (const factory of factories) {
            switch (factory.id) {
                case WidgetFactory[WidgetFactory["Controller.Symmetric.Balancing"]]:
                case WidgetFactory[WidgetFactory["Controller.Asymmetric.PeakShaving"]]:
                case WidgetFactory[WidgetFactory["Controller.Symmetric.PeakShaving"]]:
                case WidgetFactory[WidgetFactory["Controller.TimeslotPeakshaving"]]:
                case WidgetFactory[WidgetFactory["Controller.Ess.FixActivePower"]]: {

                    for (const componentId of factory.componentIds) {
                        const component = config.getComponentSafely(componentId);
                        if (component == null) {
                            continue;
                        }
                        controllerOptions.push({ value: component.id, label: component.alias });
                    }
                    break;
                }
                default:
                    break;
            }
        }

        return controllerOptions;
    }

    export class SchedulerJsCalendarPayload extends JsCalendar.OpenEMSPayload<{ controllerIds: string[] }> {

        public override update(payload: JsCalendar.OpenEMSPayload<{ controllerIds: string[]; }>, task: JsCalendar.Task<ReturnType<typeof this.toOpenEMSPayload>>) {
            const taskPayload = "openems.io:payload" in task ? task["openems.io:payload"] as { controllerIds: string[] } : null;
            const value = taskPayload != null && "controllerIds" in taskPayload ? taskPayload["controllerIds"] : null;
            if (value != null) {
                payload.setValue({ controllerIds: value });
            }
            return payload;
        }

        public override toOneTasks<Payload extends { controllerIds?: string[] }>(oneTask: OneTask<Payload>, translate: TranslateService): string | null {
            AssertionUtils.assertIsDefined(this.injector);
            const service = this.injector.get(Service);
            const edge = service.currentEdge();
            const config = edge.getConfigSignal()();
            return oneTask.payload.controllerIds?.map(controllerId => {
                return config.getComponentSafelyOrDefault(controllerId).alias;
            })?.join(",") ?? null;
        }

        public override toOpenEMSPayload(): {} {
            return { "openems.io:payload": this.value };
        }

        public override toPayloadText<T extends { controllerIds?: string[] | null; }>(translate: TranslateService): JsCalendar.Types.TaskParser<T> {
            return (value: JsCalendar.Task<T>) => {
                AssertionUtils.assertIsDefined(this.injector);
                if (value == null) {
                    return null;
                }
                const controllerOptions = SharedSchedulerJsCalendar.getControllerOptions(this.injector.get(Service));
                return controllerOptions.filter(el => StringUtils.isInArr(el.value, value["openems.io:payload"]?.controllerIds ?? null)).map(el => el.label).join(",");
            };
        }

        public override validator(translate: TranslateService): ValidationResult {
            const isValid = this.value != null;
            if (isValid) {
                return { errors: [], valid: true };
            }

            return { errors: [{ message: translate.instant("JS_SCHEDULE.ADD_ERROR"), property: this.value?.controllerIds?.toString() ?? "" }], valid: false };
        };
    }
}
