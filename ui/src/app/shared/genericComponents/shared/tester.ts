import { CurrentData } from "../../edge/currentdata";
import { ButtonLabel } from "../modal/modal-button/modal-button";
import { TextIndentation } from "../modal/modal-line/modal-line";
import { OeFormlyField, OeFormlyView } from "./oe-formly-component";

export class OeFormlyViewTester {

  public static apply(view: OeFormlyView, context: OeFormlyViewTester.Context): OeFormlyViewTester.View {
    return {
      title: view.title,
      lines: view.lines
        .map(line => OeFormlyViewTester.applyField(line, context))
        .filter(line => line)
    };
  };

  private static applyField(field: OeFormlyField, context: OeFormlyViewTester.Context): OeFormlyViewTester.Field {
    switch (field.type) {
      /**
       * OeFormlyField.Line 
       */
      case 'children-line':
        let tmp = OeFormlyViewTester.applyLineWithChildren(field, context);

        // Prepare result
        let result: OeFormlyViewTester.Field.ChildrenLine = {
          type: field.type,
          name: tmp.value
        };

        // Apply properties if available
        if (field.indentation) {
          result.indentation = field.indentation;
        }

        // Recursive call for children
        if (field.children) {
          result.children = field.children
            ?.map(child => OeFormlyViewTester.applyField(child, context));
        }

        return result;


      case "channel-line": {
        let tmp = OeFormlyViewTester.applyLineOrItem(field, context);
        if (tmp == null) {
          return null; // filter did not pass
        }

        // Read or generate name
        let name: string;
        if (typeof (field.name) === 'function') {
          name = field.name(tmp.rawValue);
        } else {
          name = field.name;
        }

        // Prepare result
        let result: OeFormlyViewTester.Field.ChannelLine = {
          type: field.type,
          name: name
        };

        // Apply properties if available
        if (tmp.value !== null) {
          result.value = tmp.value;
        }
        if (field.indentation) {
          result.indentation = field.indentation;
        }

        // Recursive call for children

        return result;
      }

      /**
       * OeFormlyField.Item
       */
      case "item": {
        let tmp = OeFormlyViewTester.applyLineOrItem(field, context);
        if (tmp == null) {
          return null; // filter did not pass
        }

        return {
          type: field.type,
          value: tmp.value
        };
      }

      /**
       * OeFormlyField.Info
       * | 
       * OeFormlyField.OnlyNameLine
       */
      case "info-line":
      case "only-name-line": {
        return {
          type: field.type,
          name: field.name
        };
      }

      /**
       * OeFormlyField.Horizontal
       */
      case "horizontal-line": {
        return {
          type: field.type
        };
      }

      /**
  * OeFormlyField.Horizontal
  */
      case "buttons-from-channel-line": {
        let value = OeFormlyViewTester.applyButtonsFromChannelLine(field, context);

        return {
          type: field.type,
          buttons: field.buttons,
          value: value
        };
      }
    }
  }

  /**
   * Common method for Line and Item as they share some fields and logic.
   * 
   * @param field the field 
   * @param context the test context
   * @returns result or null
   */
  private static applyLineOrItem(field: OeFormlyField.ChannelLine | OeFormlyField.Item, context: OeFormlyViewTester.Context):
   /* result */ { rawValue: number | null, value: string }
   /* filter did not pass */ | null {

    // Read value from channels
    let rawValue = field.channel && field.channel in context ? context[field.channel] : null;

    // Apply filter
    if (field.filter && field.filter(rawValue) === false) {
      return null;
    }

    // Apply converter
    let value: string = field.converter
      ? field.converter(rawValue)
      : rawValue === null ? null : "" + rawValue;

    return {
      rawValue: rawValue,
      value: value
    };
  }

  private static applyButtonsFromChannelLine(field: OeFormlyField.ButtonsFromChannelLine, context: OeFormlyViewTester.Context) {

    let rawValue = field.channel && field.channel in context ? context[field.channel] : null;
    let currentData = { allComponents: context };

    let value: string = field.converter
      ? field.converter(currentData)
      : rawValue === null ? null : "" + rawValue;

    return value;
  }
}

export namespace OeFormlyViewTester {

  export type Context = { [id: string]: number | null };

  export type View = {
    title: string,
    lines: Field[]
  }

  export type Field =
    | Field.InfoLine
    | Field.Item
    | Field.ChannelLine
    | Field.ChildrenLine
    | Field.HorizontalLine
    | Field.OnlyNameLine
    | Field.ButtonsFromValueLine
    | Field.ButtonsFromChannelLine;

  export namespace Field {

    export type InfoLine = {
      type: 'info-line',
      name: string
    }

    export type Item = {
      type: 'item',
      value: string
    }

    export type ChannelLine = {
      type: 'channel-line',
      name: string,
      value?: string,
      indentation?: TextIndentation,
    }

    export type ChildrenLine = {
      type: 'children-line',
      name: string,
      indentation?: TextIndentation,
      children?: Field[]
    }

    export type HorizontalLine = {
      type: 'horizontal-line',
    }

    export type OnlyNameLine = {
      type: 'only-name-line',
      name: string
    }

    export type ButtonsFromValueLine = {
      type: 'buttons-from-value-line',
      buttons: ButtonLabel[],
      value: string | number
    }

    export type ButtonsFromChannelLine = {
      type: 'buttons-from-channel-line',
      buttons: ButtonLabel[],
      value: string | number | boolean
    }
  }

  export function applyLineWithChildren(field: OeFormlyField.ChildrenLine, context: Context): { rawValue: number | null, value: string }
    | null {

    let value: string | null = null;
    let rawValue: number | null = null;

    if (typeof field.name == 'object') {
      rawValue = typeof field.name == 'object' ? (field.name.channel.toString() in context ? context[field.name.channel.toString()] : null) : null;
      value = field.name.converter(rawValue);
    }

    if (typeof (field.name) === 'string') {
      value = field.name;
    }

    return {
      rawValue: rawValue,
      value: value
    };
  }
}