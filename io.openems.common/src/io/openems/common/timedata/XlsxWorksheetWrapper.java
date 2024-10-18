package io.openems.common.timedata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.dhatim.fastexcel.StyleSetter;
import org.dhatim.fastexcel.Worksheet;

public class XlsxWorksheetWrapper {
	public record XlsxCellWrapper(int c, int r, StyleSetter style) {
	}

	private final Map<Integer, Map<Integer, XlsxCellWrapper>> cellMap = new HashMap<>();
	private final Worksheet ws;

	public XlsxWorksheetWrapper(Worksheet ws) {
		this.ws = ws;
	}

	/**
	 * Gets a CellWrapper if exists; otherwise creates a new one and returns that.
	 * 
	 * @param r row of the cell
	 * @param c column of the cell
	 * @return the XlsxCellWrapper
	 */
	public XlsxCellWrapper getCellWrapper(int r, int c) {
		final var columns = this.cellMap.computeIfAbsent(r, row -> new HashMap<>());
		return columns.computeIfAbsent(c, col -> new XlsxCellWrapper(r, c, this.ws.style(r, c)));
	}

	public void setAll() {
		this.cellMap.values().stream().flatMap(map -> map.values().stream()).forEach(val -> val.style().set());
	}

	public void setForRange(int r1, int c1, int r2, int c2, Consumer<XlsxCellWrapper> styleSetterFunc) {
		for (int row = r1; row <= r2; row++) {
			for (int col = c1; col <= c2; col++) {
				var cell = this.getCellWrapper(row, col);
				styleSetterFunc.accept(cell);;
			}
		}
	}

}