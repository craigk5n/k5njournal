/*
 * Copyright (C) 2005-2024 Craig Knudsen
 *
 * k5nJournal is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * A copy of the GNU Lesser General Public License can be found at www.gnu.org. 
 * To receive a hard copy, you can write to:
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA.
 */

package us.k5n.journal;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Overide methods of JTable to customize: no cell editing, alternating
 * background colors for odd/even rows, resize to fit
 */
public class ReadOnlyTable extends JTable {
	private static final long serialVersionUID = 1L;
	int[] highlightedRows = null;
	Color lightGray;
	private boolean firstPaint = true;
	private HashMap<Integer, Integer> fixedWidths = new HashMap<Integer, Integer>();

	public void clearHighlightedRows() {
		this.highlightedRows = null;
	}

	public void setHighlightedRow(int row) {
		this.highlightedRows = new int[1];
		highlightedRows[0] = row;
		repaint();
	}

	public void setHighlightedRows(int[] rows) {
		this.highlightedRows = rows;
		repaint();
	}

	private boolean rowIsHighlighted(int row) {
		if (highlightedRows == null)
			return false;
		for (int i = 0; i < highlightedRows.length; i++) {
			if (highlightedRows[i] == row)
				return true;
		}
		return false;
	}

	public void setColumnFixedWidth(int col, int width) {
		this.fixedWidths.put(new Integer(col), new Integer(width));
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
		if (renderer == null) {
			renderer = getDefaultRenderer(getColumnClass(vColIndex));
		}
		if (renderer == null)
			return null;

		Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);

		if (rowIsHighlighted(rowIndex)) {
			c.setBackground(Color.blue);
			c.setForeground(Color.white);
		} else if (rowIndex % 2 == 0) {
			c.setBackground(lightGray);
			c.setForeground(Color.black);
		} else {
			c.setBackground(getBackground());
			c.setForeground(Color.black);
		}

		return c;
	}

	public ReadOnlyTable(int rows, int cols) {
		super(rows, cols);
		lightGray = new Color(220, 220, 220);
	}

	public ReadOnlyTable(TableModel tm) {
		super(tm);
		lightGray = new Color(220, 220, 220);
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void paint(Graphics g) {
		if (firstPaint) {
			// After first paint call, we can get font metric info. So,
			// call autoResize to adjust column widths.
			autoResize();
			// doLayout ();
			firstPaint = false;
		}
		super.paint(g);
	}

	private void autoResize() {
		final int viewWidth = this.getVisibleRect().width;
		int numVariableW = 0;
		FontMetrics fm = getGraphics().getFontMetrics();
		int[] widths = new int[getColumnCount()];
		for (int i = 0; i < getColumnCount(); i++) {
			Integer colInt = new Integer(i);
			if (this.fixedWidths.containsKey(colInt)) {
				widths[i] = fixedWidths.get(colInt).intValue();
			} else {
				numVariableW++;
				TableColumn tc = this.getColumnModel().getColumn(i);
				Object o = tc.getHeaderValue();
				if (o instanceof String) {
					String label = (String) tc.getHeaderValue();
					widths[i] = fm.stringWidth(label) + 2
							* getColumnModel().getColumnMargin() + 15;
				} else if (o instanceof ImageIcon) {
					ImageIcon icon = (ImageIcon) o;
					widths[i] = icon.getIconWidth() + 5;
				} else {
					// Assume some default size
					widths[i] = 20;
				}
			}
		}
		for (int row = 0; row < getRowCount(); row++) {
			for (int col = 0; col < getColumnCount(); col++) {
				Object o = this.getValueAt(row, col);
				int width = 0;
				if (o instanceof String) {
					String val = this.getValueAt(row, col).toString();
					width = (val == null ? 0
							: fm.stringWidth(val)
									+ (2 * getColumnModel().getColumnMargin()) + 6);
				} else if (o instanceof ImageIcon) {
					ImageIcon icon = (ImageIcon) o;
					width = icon.getIconWidth() + 5;
				}
				if (width > widths[col])
					widths[col] = width;
			}
		}

		int totalW = 0;
		for (int i = 0; i < getColumnCount(); i++) {
			totalW += widths[i];
		}
		// Dimension d = getPreferredSize ();
		// d = new Dimension ( totalW + 50, d.height );
		// setPreferredSize ( d );
		// How much extra space do we have?
		int extraW = viewWidth - totalW;
		if (extraW < 0)
			extraW = 0;
		int padding = numVariableW == 0 ? 0 : extraW / numVariableW;
		for (int i = 0; i < getColumnCount(); i++) {
			int newWidth = widths[i];
			TableColumn tc = getColumnModel().getColumn(i);
			if (!this.fixedWidths.containsKey(new Integer(i)))
				newWidth += padding;
			tc.setPreferredWidth(newWidth);
			tc.setWidth(newWidth);
			// System.out.println ( "Setting col#" + i + " width=" + widths[i] );
		}
	}

	// Implement table header tool tips.
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			private static final long serialVersionUID = 1L;

			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				Object o = columnModel.getColumn(index).getHeaderValue();
				String text = null;
				if (o instanceof String)
					text = (String) o;
				else
					text = "this column";
				return "Click to sort by " + text;
			}
		};
	}
}
