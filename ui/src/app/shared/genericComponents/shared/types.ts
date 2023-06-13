import { TextIndentation } from "../modal/modal-line/modal-line";
export type ModalField =
  | ModalFieldLine
  | ModalFieldLineInfo
  | ModalFieldLineItem

type ModalFieldLineInfo = {
  type: 'line-info',
  name: string
}
type ModalFieldLineItem = {
  type: 'line-item',
  channel: string,
  converter?: Function,
  indentation?: TextIndentation,
  filter?: Function,
}

type ModalFieldLine = {
  type: 'line',
  name: string | Function,
  filter?: Function,
  channel?: string,
  converter?: Function,
  indentation?: TextIndentation,
  children?: ModalField[]
}