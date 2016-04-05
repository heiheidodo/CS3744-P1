import java.util.Arrays;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

/**
 * Homework 5: Custom model.
 *
 * @author Denis Gracanin
 * @version 1
 */
public class PR1Model extends DefaultTableModel {
	private static final long serialVersionUID = 1L;
	private float vertexData[] = null;
	private float minValues[] = null;
	private float maxValues[] = null;
	private float left[] = null;
	private float right[] = null;
	private float bottom[] = null;
	private float top[] = null;
	private float hleft[] = null;
	private float hright[] = null;
	private float hbottom[] = null;
	private float htop[] = null;
	private int hcount[] = null;
	private int plotSize = 0;
	private int plotCount = 0;
	private boolean isError = false;
	private int oldColumn = -1;
	private int oldRow = -1;
	private int padding = 0;

	/**
	 * Create an instance of <code>HW4Model</code> class.
	 */
	public PR1Model() {
		padding = 0;
		close();
	}

	public float[] getHistogramData(int c, int n, float minv, float maxv) {
		if (n == 0) n = 1;
		int [] temp = new int[n];
		for (int i = 0; i < temp.length; i++) {
			temp[i] = 0;
		}
		for (int i = 0; i < plotSize; i++) {
			float f = 0.0f;
			Object o = null;
			try {
				o = getValueAt(i, c);
			}
			catch (ArrayIndexOutOfBoundsException e) {
				o = null;
			}
			if (o != null) {
				f = Float.parseFloat(o.toString());
			}
			int j = (int) Math.floor((f - minv) / (maxv - minv) * n);
			if (j >= temp.length) {
				j = temp.length - 1;
			}
			if (j < 0) {
				j = 0;
			}
			temp[j]++;
		}
		float[] data = new float[12 * temp.length + padding];
		int max = 0;
		for (int i = 0; i < temp.length; i++) {
			if (temp[i] > max) {
				max = temp[i];
			}
		}
		for (int i = 0; i < temp.length; i++) {
			data[12 * i + 0] = i;
			data[12 * i + 1] = temp[i];
			data[12 * i + 2] = i;
			data[12 * i + 3] = 0;
			data[12 * i + 4] = i + 1;
			data[12 * i + 5] = 0;

			data[12 * i + 6] = i;
			data[12 * i + 7] = temp[i];
			data[12 * i + 8] = i + 1;
			data[12 * i + 9] = 0;
			data[12 * i + 10] = i + 1;
			data[12 * i + 11] = temp[i];
		}
		return data;
	}

	/**
	 * Return the padded vertex data array.
	 * The extra (padding) vertex data array elements are not initialized.
	 *
	 * @return Padded vertex data array.
	 */
	public float[] getVertexData() {
		return Arrays.copyOf(vertexData, vertexData.length + padding);
	}

	/**
	 * Return the minimum value for the specified plot (column).
	 * If the specified plot is out of range, 0 is returned.
	 *
	 * @param i The plot (column) index.
	 * @return Minimum value
	 */
	public float getMinValue(int i) {
		return (i >=0 && i < minValues.length ? minValues[i] : 0);
	}

	/**
	 * Return the maximum value for the specified plot (column).
	 * If the specified plot is out of range, 0 is returned.
	 *
	 * @param i The plot (column) index.
	 * @return Maximum value
	 */
	public float getMaxValue(int i) {
		return (i >=0 && i < maxValues.length ? maxValues[i] : 0);
	}

	/**
	 * Return the plot size (number of rows in the model).
	 *
	 * @return Plot size.
	 */
	public int getPlotSize() {
		return plotSize;
	}

	/**
	 * Return the plot count (number of columns in the model).
	 *
	 * @return Plot count.
	 */
	public int getPlotCount() {
		return plotCount;
	}

	/**
	 * Clear the vertex data.
	 */
	public void close() {
		setRowCount(0);
		setColumnCount(0);
		clear();
	}

	/**
	 * Clear the vertex data.
	 */
	public void clear() {
		isError = false;
		oldColumn = -1;
		oldRow = -1;
		plotSize = 0;
		plotCount = 0;
		vertexData = new float[0];
		minValues = new float[0];
		maxValues = new float[0];
		left = new float[0];
		right = new float[0];
		bottom = new float[0];
		top = new float[0];
		hleft = new float[0];
		hright = new float[0];
		hbottom = new float[0];
		htop = new float[0];
		hcount = new int[0];
	}

	/**
	 * Overriden to check if the new value is a number before updating the model.
	 *
	 * @param value New cell value.
	 * @param row The row of the cell.
	 * @param col The column of the cell.
	 */
	@Override
	public void setValueAt(Object value, int row, int col) {
		oldRow = row;
		oldColumn = col;
		try {
			Float.parseFloat((String) value);
			super.setValueAt(value, row, col);
			setError(false);
		}
		// Not a number
		catch (NumberFormatException e) {
			setError(true);
		}
	}

	/**
	 * Populate the vertex data array (used by OpenGL) with the new model values.
	 *
	 * @param e Table model event.
	 */
	@Override
	public void fireTableChanged(TableModelEvent e) {
		@SuppressWarnings("unchecked")
		Vector<Vector<String>> data = (Vector<Vector<String>>) getDataVector();
		int c = e.getColumn();
		int r1 = e.getFirstRow();
		int r2 = e.getLastRow();
		plotSize = data.size();
		if (c >= 0 && r1 >= 0 && r1== r2) {
			float f = Float.parseFloat(data.get(r1).get(c));
			if (Math.abs(f - vertexData[2 * (c * plotSize + r1) + 1]) < 10 * Float.MIN_VALUE) {
				return;
			}
		}
		if (plotSize > 0) {
			plotCount = data.get(0).size();
			minValues = new float[plotCount];
			maxValues = new float[plotCount];
			left = new float[plotCount];
			right = new float[plotCount];
			bottom = new float[plotCount];
			top = new float[plotCount];
			hleft = new float[plotCount];
			hright = new float[plotCount];
			hbottom = new float[plotCount];
			htop = new float[plotCount];
			hcount = new int[plotCount];
			vertexData = new float[2 * plotCount * plotSize];
			for (int i = 0; i < plotCount; i++) {
				minValues[i] = Float.MAX_VALUE;
				maxValues[i] = -1.0f * Float.MAX_VALUE;
				for (int j = 0; j < plotSize; j++) {
					float f = Float.parseFloat(data.get(j).get(i));
					vertexData[2 * (i * plotSize + j)] = j;
					vertexData[2 * (i * plotSize + j) + 1] = f;
					if (f < minValues[i]) {
						minValues[i] = f;
					}
					if (f > maxValues[i]) {
						maxValues[i] = f;
					}
				}
				hbottom[i] = bottom[i] = minValues[i];
				htop[i] = top[i] = maxValues[i];
				hleft[i] = left[i] = 0;
				right[i] = plotSize -1;
				hcount[i] = 8;
				int bins[] = new int[hcount[i]];
				for (int j = 0; j < hcount[i]; j++) {
					bins[j] = 0;
				}
				for (int j = 0; j < plotSize; j++) {
					float f = Float.parseFloat(data.get(j).get(i));
					int k = (int) Math.floor((f - hbottom[i]) / (htop[i] - hbottom[i]) * hcount[i]);
					if (k >= bins.length) {
						k = bins.length - 1;
					}
					bins[k]++;
				}
				hright[i] = 0;
				for (int j = 0; j < bins.length; j++) {
					if (hright[i] < bins[j]) {
						hright[i] = bins[j];
					}
				}
//				System.out.println(hright[i]);
			}
		}
		else {
			clear();
		}
		super.fireTableChanged(e);
	}

	public int getMaxBin(int i, int c) {
		int r;
		Vector<Vector<String>> data = (Vector<Vector<String>>) getDataVector();
		int bins[] = new int[c];
		for (int j = 0; j < c; j++) {
			bins[j] = 0;
		}
		for (int j = 0; j < plotSize; j++) {
			float f = Float.parseFloat(data.get(j).get(i));
			int k = (int) Math.floor((f - minValues[i]) / (maxValues[i] - minValues[i]) * c);
			if (k >= bins.length) {
				k = bins.length - 1;
			}
			bins[k]++;
		}
		r = 0;
		for (int j = 0; j < bins.length; j++) {
			if (r < bins[j]) {
				r = bins[j];
			}
		}
		return r;
	}


	/**
	 * Set the status of the last invocation of the setValueAt method.
	 * true - Number format error.
	 * false - No error.
	 *
	 * @param e Error status.
	 */
	public void setError(boolean e) {
		isError = e;
	}

	/**
	 * Return the status of the last invocation of the setValueAt method.
	 * true - Number format error.
	 * false - No error.
	 *
	 * @return Error status.
	 */
	public boolean isError() {
		return isError;
	}

	/**
	 * Return the row used in the last invocation of setValueAt.
	 *
	 * @return Old row.
	 */
	public int getOldRow() {
		return oldRow;
	}

	/**
	 * Return the column used in the last invocation of setValueAt.
	 *
	 * @return Old column.
	 */
	public int getOldColumn() {
		return oldColumn;
	}

	/**
	 * Return the current padding value.
	 *
	 * @return The padding.
	 */
	public int getPadding() {
		return padding;
	}

	/**
	 * Set the current padding value (the number of extra vertex data array elements).
	 *
	 * @param padding the padding to set
	 */
	public void setPadding(int p) {
		padding = p < 0 ? 0 : p;
	}

	/**
	 * Set the current plot limits.
	 *
	 * @param i The plot index.
	 * @param l The smallest data point index.
	 * @param r The largest data point index.
	 * @param b The smallest data point values.
	 * @param t The largest data point value.
	 */
	public void setCurrent(int i, float l, float r, float b, float t) {
		if (i >=0 && i < maxValues.length) {
			left[i] = l;
			right[i] = r;
			bottom[i] = b;
			top[i] = t;
		}
	}

	public void setHistogramCurrent(int i, float l, float r, float b, float t, int c) {
		if (i >=0 && i < maxValues.length) {
			hleft[i] = l;
			hright[i] = r;
			hbottom[i] = b;
			htop[i] = t;
			hcount[i] = c;
		}
	}

	/**
	 * Return the minimum data point index value for the specified plot (column).
	 * If the specified plot is out of range, 0 is returned.
	 *
	 * @param i The plot (column) index.
	 * @return Minimum value
	 */
	public float getLeft(int i) {
		return (i >=0 && i < left.length ? left[i] : 0);
	}

	/**
	 * Return the maximum data point index value for the specified plot (column).
	 * If the specified plot is out of range, 0 is returned.
	 *
	 * @param i The plot (column) index.
	 * @return Maximum value
	 */
	public float getRight(int i) {
		return (i >=0 && i < right.length ? right[i] : 0);
	}

	/**
	 * Return the minimum data point value for the specified plot (column).
	 * If the specified plot is out of range, 0 is returned.
	 *
	 * @param i The plot (column) index.
	 * @return Minimum value
	 */
	public float getBottom(int i) {
		return (i >=0 && i < bottom.length ? bottom[i] : 0);
	}

	/**
	 * Return the maximum data point value for the specified plot (column).
	 * If the specified plot is out of range, 0 is returned.
	 *
	 * @param i The plot (column) index.
	 * @return Maximum value
	 */
	public float getTop(int i) {
		return (i >=0 && i < top.length ? top[i] : 0);
	}

	public int getBinPoint(int b, int c, int count, float minv, float maxv) {
		for (int i = 0; i < plotSize; i++) {
			float f = 0.0f;
			Object o = null;
			try {
				o = getValueAt(i, c);
			}
			catch (ArrayIndexOutOfBoundsException e) {
				o = null;
			}
			if (o != null) {
				f = Float.parseFloat(o.toString());
			}
			int j = (int) Math.floor((f - minv) / (maxv - minv) * count);
			if (j >= count) {
				j = count - 1;
			}
			if (j < 0) {
				j = 0;
			}
			if (b == j) {
				return i;
			}

		}
		return -1;
	}


	public int getPointBin(int p, int c, int count, float minv, float maxv) {
		float f = 0.0f;
		int j = -1;
		Object o = null;
		try {
			o = getValueAt(p, c);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			o = null;
		}
		if (o != null) {
			f = Float.parseFloat(o.toString());
			j = (int) Math.floor((f - minv) / (maxv - minv) * count);
			if (j >= count) {
				j = count - 1;
			}
			if (j < 0) {
				j = 0;
			}
		}
		return j;
	}

	public float getHLeft(int i) {
		return (i >=0 && i < hleft.length ? hleft[i] : 0);
	}

	public float getHRight(int i) {
		return (i >=0 && i < hright.length ? hright[i] : 0);
	}

	public float getHBottom(int i) {
		return (i >=0 && i < hbottom.length ? hbottom[i] : 0);
	}

	public float getHTop(int i) {
		return (i >=0 && i < htop.length ? htop[i] : 0);
	}

	public int getHCount(int i) {
		return (i >=0 && i < hcount.length ? hcount[i] : 0);
	}

}
