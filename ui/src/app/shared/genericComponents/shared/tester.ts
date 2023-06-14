import { TextIndentation } from "../modal/modal-line/modal-line";
import { OeFormlyField, OeFormlyView } from "./oe-formly-component";

export namespace OeFormlyViewTester {

  export type Channels = { [id: string]: number | null };

  export type View = {
    title: string,
    lines: Field[]
  }

  export type Field =
    | Field.Line
    | Field.Info
    | Field.Item
    | Field.Horizontal;

  export namespace Field {

    export type Info = {
      type: 'line-info',
      name: string
    }

    export type Item = {
      type: 'line-item',
      value: string
    }

    export type Line = {
      type: 'line',
      name: string,
      value?: string,
      indentation?: TextIndentation,
      children?: Field[]
    }

    export type Horizontal = {
      type: 'line-horizontal',
    }
  }
}

export class OeFormlyViewTester {

  public static apply(view: OeFormlyView, channels: OeFormlyViewTester.Channels): OeFormlyViewTester.View {
    return {
      title: view.title,
      lines: view.lines
        .map(line => OeFormlyViewTester.applyField(line, channels))
        .filter(line => line)
    }
  };

  private static applyField(field: OeFormlyField, channels: OeFormlyViewTester.Channels): OeFormlyViewTester.Field {
    switch (field.type) {
      /**
       * OeFormlyField.Line 
       */
      case "line": {
        let tmp = OeFormlyViewTester.applyLineOrItem(field, channels);
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
        let result: OeFormlyViewTester.Field.Line = {
          type: field.type,
          name: name,
        }

        // Apply properties if available
        if (tmp.value !== null) {
          result.value = tmp.value;
        }
        if (field.indentation) {
          result.indentation = field.indentation;
        }

        // Recursive call for children
        if (field.children) {
          result.children = field.children
            ?.map(field => OeFormlyViewTester.applyField(field, channels));
        }

        return result;
      }

      /**
       * OeFormlyField.Item
       */
      case "line-item": {
        let tmp = OeFormlyViewTester.applyLineOrItem(field, channels);
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
       */
      case "line-info": {
        return {
          type: field.type,
          name: field.name
        }
      }

      /**
       * OeFormlyField.Horizontal
       */
      case "line-horizontal": {
        return {
          type: field.type
        }
      }
    }
  }

  private static applyLineOrItem(line: OeFormlyField.Line | OeFormlyField.Item, channels: OeFormlyViewTester.Channels): { rawValue: number | null, value: string } | null {
    // Read value from channels
    let rawValue = line.channel && line.channel in channels ? channels[line.channel] : null;

    // Apply filter
    if (line.filter && line.filter(rawValue) === false) {
      return null;
    }

    // Apply converter
    let value: string = line.converter
      ? line.converter(rawValue)
      : rawValue === null ? null : "" + rawValue;

    return {
      rawValue: rawValue,
      value: value
    };
  }
}