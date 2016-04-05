import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.jogamp.common.nio.Buffers;

/**
 * Project 1: A histogram view (shader based) of the model data.
 *
 * @author Denis Gracanin
 * @version 1
 */
public class PR1Histogram extends JComponent implements ActionListener, FocusListener, GLEventListener, TableModelListener {
	private static final long serialVersionUID = 1L;
	private static int VERTEX_DATA_PADDING = 28;
	private PR1Model model = null;
	private GLJPanel panel = null;
	private JPanel textPanel = null;
	private JButton resetButton = null;
	private JTextField textLeft = null;
	private JTextField textRight = null;
	private JTextField textBottom = null;
	private JTextField textTop = null;
	private JTextField textCount = null;
	private Color backgroundColor = Color.WHITE;
	private int fragmentShader = 0;
	private int vertexShader = 0;
	private int shaderProgram = 0;
	private int pointSelection = -1;
	private int binSelection = -1;
	private int plotSize = 0;
	private int plotSelection = -1;
	private int oldX = 0;
	private int oldY = 0;
	private boolean selectionRectangle = false;
	private int vertexType = 0;
	private int[] counts = null;
	private float left = -1.0f;
	private float right = 1.0f;
	private float bottom = -1.0f;
	private int button = 0;
	private float top = 1.0f;
	private int count = 8;
	private float offsetX = 0.0f;
	private ArrayList<ChangeListener> listeners = null;
	private static final String VERTEX_SHADER =
			"#version 150\n" +
					"in vec4 vPosition;\n" +
					"uniform int binSelection;\n" +
					"uniform float minY;\n" +
					"uniform float maxY;\n" +
					"uniform float minX;\n" +
					"uniform float maxX;\n" +
					"uniform float offsetX;\n" +
					"uniform int numberOfBins;\n" +
					"uniform int vertexType;\n" +
					"out vec4 vColor;\n" +
					"\n" +
					"void main(void) {\n" +
					"  switch (vertexType) {\n" +
					"  case 2:\n" + // DRAW RUBBERBAND RECTANGLE
					"    gl_Position = vec4(vPosition.x, vPosition.y, 0.0, 1.0);\n" +
					"    vColor = vec4(0.0, 1.0, 0.0, 1.0);\n" +
					"    break;\n" +
					"  case 1:\n" + // DRAW COORDINATE AXES
					"    gl_Position = vec4(vPosition.x, vPosition.y, 0.0, 1.0);\n" +
					"    vColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
					"    break;\n" +
					"  default:\n" + // DRAW PLOT
					"    float x = offsetX + -0.8 + 1.6 * (vPosition.x) / numberOfBins;\n" +
					"    float y = -0.8 + 1.6 * (vPosition.y - minY) / (maxY - minY);\n" +
					"     gl_Position = vec4(x , y, 0.0, 1.0);\n" +
					"    if (binSelection == gl_VertexID / 6) {" +
					"      vColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
					"    }\n" +
					"    else {\n" +
					"      vColor = vec4(0.0, 0.0, 1.0, 1.0);" +
					"    }\n" +
					"  }\n" +
					"}\n";
	private static final String FRAGMENT_SHADER =
			"#version 150\n" +
					"in vec4 vColor;\n" +
					"out vec4 fColor;\n" +
					"\n" +
					"void main(void) {\n" +
					"  fColor = vColor;\n" +
					"}\n";

	/**
	 * Stores the vertex data, 2 values per vertex:
	 * 1st value: x coordinate (row number in the table)
	 * 2nd value: y coordinate (cell value)
	 */
	private float vertexData[]= null;

	/**
	 * Used for Vertex Buffer Object (VBO).
	 */
	private IntBuffer intBuffer = null;

	/**
	 * Used for Vertex Array Object (VAO).
	 */
	private FloatBuffer floatBuffer = null;
	private int location;
	private float rubberBandData[] = new float[8];

	/**
	 * Creates an instance of <code>HW5Plot</code> with the specified values for scale and color.
	 *
	 * @param args an array of strings to be displayed
	 */
	public PR1Histogram(PR1Model m) {
		super();
		model = m;
		model.setPadding(VERTEX_DATA_PADDING);
		model.addTableModelListener(this);
		listeners = new ArrayList<ChangeListener>();
		panel = new GLJPanel(new GLCapabilities(GLProfile.getMaxProgrammable(false)));
		panel.addGLEventListener(this);

		// MouseAdapter is used to handle mouse events.
		panel.addMouseListener(
		/**
		 * Mouse adapter class.
		 *
		 * @author Denis Gracanin
		 * @ version 1
		 */
		new MouseAdapter() {

			/**
			 * Set the first vertex of the rubberband rectangle.
			 *
			 * @param e Mouse event.
			 */
			public void mousePressed(MouseEvent e) {
				button = e.getButton();
				if (button == MouseEvent.BUTTON3) {
					oldX = e.getX();
					oldY = e.getY();
					return;
				}
				selectionRectangle = true;
				counts = new int[count];
				for (int i = 0; i < counts.length; i++) {
					counts[i] = 0;
				}
				rubberBandData[0] = -1.0f + e.getX() * 2.0f / panel.getWidth();
				rubberBandData[1] = 1.0f - e.getY() * 2.0f / panel.getHeight();
			}

			/**
			 * Remove the rubberband rectangle.
			 *
			 * @param e Mouse event.
			 */
			public void mouseReleased(MouseEvent e) {
				selectionRectangle = false;
				repaint();
			}

		});
		panel.addMouseMotionListener(
				/**
				 * Mouse motion adapter class.
				 *
				 * @author Denis Gracanin
				 * @ version 1
				 */
			new MouseMotionAdapter() {

			/**
			 * Draw the rubberband rectangle
			 *
			 * @param e Mouse event.
			 */
			public void mouseDragged(MouseEvent e) {
				if (button == MouseEvent.BUTTON3) {
					float deltaY = (e.getY() - oldY) * 2.0f / panel.getHeight() * (right - left) / 1.6f;
					textLeft.setText(Float.toString(left + deltaY));
					textRight.setText(Float.toString(right + deltaY));
					float deltaX = (oldX - e.getX()) * 2.0f / panel.getWidth();
					offsetX -= deltaX;
					oldX = e.getX();
					oldY = e.getY();
					update();
					repaint();
					return;
				}
				rubberBandData[4] = -1.0f + e.getX() * 2.0f / panel.getWidth();
				rubberBandData[5] = 1.0f - e.getY() * 2.0f / panel.getHeight();
				rubberBandData[2] = rubberBandData[0];
				rubberBandData[3] = rubberBandData[5];
				rubberBandData[6] = rubberBandData[4];
				rubberBandData[7] = rubberBandData[1];

				if (plotSelection == -1) {
					return;
				}
				int maxIndex = -1;
				int max = 0;
				float xMin = Math.min(rubberBandData[0], rubberBandData[4]);
				float xMax = Math.max(rubberBandData[0], rubberBandData[4]);
				float yMin = Math.min(rubberBandData[1], rubberBandData[5]);
				float yMax = Math.max(rubberBandData[1], rubberBandData[5]);
				for (int i = 0; i < counts.length; i++) {
					float x1 = offsetX + -0.8f + 1.6f * i / counts.length;
					float x2 = offsetX + -0.8f + 1.6f * (i + 1) / counts.length;
					float y1 = -0.8f + 1.6f * (0.0f - left) / (right - left);
					float y2 = -0.8f + 1.6f * (vertexData[12 * i + 1] - left) / (right - left);
					if (!(
						((x1 <= xMin && x2 <= xMin) || (x1 >= xMax && x2 >= xMax))
						||
						((y1 <= yMin && y2 <= yMin) || (y1 >= yMax && y2 >= yMax))
						)) {
						counts[i]++;
					}
					else {
						counts[i] = 0;
					}
					if (counts[i] > max) {
						max = counts[i];
						maxIndex = i;
					}
				}
				if (maxIndex >= 0) {
					setBinSelection(maxIndex);
				}
				repaint();
			}

		});

		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);

		textPanel = new JPanel(new GridLayout(4,3));

		textPanel.add(new JLabel("", JLabel.CENTER));
		textPanel.add(new JLabel("No. of Bins", JLabel.CENTER));
		textPanel.add(textCount = getTextField(count, "c", this));

		resetButton = new JButton("Default");
		resetButton.setActionCommand("d");
		resetButton.addActionListener(this);
		textPanel.add(resetButton);

		textPanel.add(new JLabel("Min", JLabel.CENTER));
		textPanel.add(new JLabel("Max", JLabel.CENTER));
		textPanel.add(new JLabel("Bin Value", JLabel.CENTER));
		textPanel.add(textBottom = getTextField(0.0f, "b", this));
		textPanel.add(textTop = getTextField(0.0f, "t", this));
		textPanel.add(new JLabel("Bin Count", JLabel.CENTER));
		textPanel.add(textLeft = getTextField(0.0f, "l", this));
		textPanel.add(textRight = getTextField(0.0f, "r", this));
		textCount.setText("8");

		add(textPanel, BorderLayout.SOUTH);
		vertexData = model.getVertexData();
		addAxes();
	}

	private void addRubberBand() {
		for (int i = 0; i < 8; i++) {
			vertexData[vertexData.length - 8 + i] = rubberBandData[i];
		}

	}

	/**
	 * Creates the shader program from source.
	 *
	 * @param drawable OpenGL drawable.
	 */
	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();

		vertexShader = compile(gl, GL3.GL_VERTEX_SHADER, VERTEX_SHADER);
		fragmentShader = compile(gl, GL3.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
		shaderProgram = gl.glCreateProgram();
		gl.glAttachShader(shaderProgram, vertexShader);
		gl.glAttachShader(shaderProgram, fragmentShader);
		gl.glLinkProgram(shaderProgram);
	}

	/**
	 * Overridden to draw the graph in the drawable area.
	 */
	@Override
	public void display(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();

		// Sets the background color.
		gl.glClearColor(backgroundColor.getRed() / 255.0f, backgroundColor.getGreen() / 255.0f, backgroundColor.getBlue() / 255.0f, backgroundColor.getAlpha() / 255.0f);
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT);

		// Enable using gl_PointSize in the vertex shader.
		gl.glEnable(GL3.GL_VERTEX_PROGRAM_POINT_SIZE);

		// Use the shader
		gl.glUseProgram(shaderProgram);

		// Set the VBO.
		intBuffer = Buffers.newDirectIntBuffer(1);
		gl.glGenBuffers(1, intBuffer);
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, intBuffer.get(0));

		// Set the VAO.
		vertexData = model.getHistogramData(plotSelection, count, bottom, top);
		addAxes();
		addRubberBand();
		floatBuffer = Buffers.newDirectFloatBuffer(vertexData);
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, vertexData.length * Buffers.SIZEOF_FLOAT, floatBuffer, GL3.GL_STATIC_DRAW);

		// Use vertex data in the vertexData array as vPosition vertex shader variable.
		location = gl.glGetAttribLocation(shaderProgram, "vPosition");
		gl.glVertexAttribPointer(location, 2, GL3.GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(location);

		// Use the current value of plotSize  variable as plotSize vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "minX");
		gl.glUniform1f(location, bottom);

		// Use the current value of plotSize  variable as plotSize vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "maxX");
		gl.glUniform1f(location, top);

		// Use the current value of minVal  variable as minVal vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "minY");
		gl.glUniform1f(location, left);

		// Use the current value of maxVal  variable as maxVal vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "maxY");
		gl.glUniform1f(location, right);


		// Use the current value of maxVal  variable as maxVal vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "offsetX");
		gl.glUniform1f(location, offsetX);

		// Use the current value of maxVal  variable as maxVal vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "numberOfBins");
		gl.glUniform1i(location, count);

		// Use the current value of pointSelection  variable as pointSelection vertex shader variable.
		location = gl.glGetUniformLocation(shaderProgram, "binSelection");
		gl.glUniform1i(location, binSelection);
		update();


		// Plot selected?
		if (plotSelection >= 0) {

			// Draw coordinate axes
			vertexType = 1;
			location = gl.glGetUniformLocation(shaderProgram, "vertexType");
			gl.glUniform1i(location, vertexType);
			gl.glDrawArrays(GL3.GL_LINES, 6 * count, 4);
			gl.glDrawArrays(GL3.GL_TRIANGLES, 6 * count + 4, 6);

			vertexType = 0;
			location = gl.glGetUniformLocation(shaderProgram, "vertexType");
			gl.glUniform1i(location, vertexType);

			// Draw individual points (GL_POINTS primitive).
			// Specify the starting vertex index and the number of vertices to be used for the primitive drawn.
			gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6 * count);


			// DRAW RUBBERBAND RECTANGLE
			if (selectionRectangle) {
				vertexType = 2;
				location = gl.glGetUniformLocation(shaderProgram, "vertexType");
				gl.glUniform1i(location, vertexType);
				// Draw line strip(GL_LINE_STRIP primitive).
				// Specify the starting vertex index and the number of vertices to be used for the primitive drawn.
				gl.glDrawArrays(GL3.GL_LINE_LOOP, 6 * count + 10, 4);
			}
		}

	}

	/**
	 * Detaches and deletes the created shaders and the shader program..
	 *
	 * @param drawable OpenGL drawable.
	 */
	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();
		gl.glDetachShader(shaderProgram, vertexShader);
		gl.glDeleteShader(vertexShader);
		gl.glDetachShader(shaderProgram, fragmentShader);
		gl.glDeleteShader(fragmentShader);
		gl.glDeleteProgram(shaderProgram);
	}

	/**
	 * Overridden as an empty method.
	 *
	 * @param drawable OpenGL drawable.
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}

	/**
	 * Draws the graph with the specified data sets and the selected point.
	 *
	 * @param plot the selected column in the table view
	 * @param point the selected row in the table view
	 */
	public void draw(int plot, int point) {
		int bin = model.getPointBin(point, plot, count, bottom, top);
		if (plot != plotSelection || bin != binSelection) {
			plotSelection = plot;
			pointSelection = point;
			binSelection = bin;
//			System.out.println(plotSelection + " - " + model.getHRight(plotSelection));
			textLeft.setText(Float.toString(model.getHLeft(plotSelection)));
			textRight.setText(Float.toString(model.getHRight(plotSelection)));
			textBottom.setText(Float.toString(model.getHBottom(plotSelection)));
			textTop.setText(Float.toString(model.getHTop(plotSelection)));
			textCount.setText(Integer.toString(model.getHCount(plotSelection)));
			update();
			repaint();
		}
	}

	/**
	 * Sets the background color.
	 *
	 * @param bc the background color to set
	 */
	@Override
	public void setBackground(Color bc) {
		backgroundColor = bc;
	}

	/**
	 * A utility method to create a shader
	 *
	 * @param gl The OpenGL context.
	 * @param shaderType The type of the shader.
	 * @param program The string containing the program.
	 * @return the created shader.
	 */
	private int compile(GL3 gl, int shaderType, String program) {
		int shader = gl.glCreateShader(shaderType);
		String[] lines = new String[] { program };
		int[] lengths = new int[] { lines[0].length() };
		gl.glShaderSource(shader, lines.length, lines, lengths, 0);
		gl.glCompileShader(shader);
		int[] compiled = new int[1];
		gl.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, compiled, 0);
		if(compiled[0]==0) {
			int[] logLength = new int[1];
			gl.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, logLength, 0);
			byte[] log = new byte[logLength[0]];
			gl.glGetShaderInfoLog(shader, logLength[0], (int[])null, 0, log, 0);
			System.err.println("Error compiling the shader: " + new String(log));
			System.exit(1);
		}
		return shader;
	}

	/**
	 * Updates the vertex data based on the data in the model.
	 *
	 * @param e table mode vent
	 */
	@Override
	public void tableChanged(TableModelEvent e) {
		plotSize = model.getPlotSize();
		textLeft.setText(Float.toString(model.getHLeft(plotSelection)));
		textRight.setText(Float.toString(model.getHRight(plotSelection)));
		textBottom.setText(Float.toString(model.getHBottom(plotSelection)));
		textTop.setText(Float.toString(model.getHTop(plotSelection)));
		textCount.setText(Integer.toString(model.getHCount(plotSelection)));
		update();
		repaint();
	}

	/**
	 * Adds a change listener.
	 *
	 * @param l change listener
	 */
	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}

	/**
	 * Removes a change listener.
	 *
	 * @param l change listener
	 */
	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	/**
	 * Fires a change event.
	 */
	protected void fireStateChanged() {
		ChangeEvent e = new ChangeEvent(this);
		for (ChangeListener l : listeners) {
			l.stateChanged(e);
		}
	}

	/**
	 * Return the selected point value.
	 *
	 * @return point index.
	 */
	public int getValue() {
		return pointSelection;
	}

	/**
	 * Set the selected point value.
	 *
	 * @param i point index.
	 */
	private void setPointSelection(int i) {
		pointSelection = i;
		fireStateChanged();
	}

	private void setBinSelection(int i) {
		binSelection = i;
		setPointSelection(model.getBinPoint(i , plotSelection, count, bottom, top));
	}


	/**
	 * Construct coordinate axes.
	 */
	private void addAxes() {
		int valueCount = vertexData.length - 28;

		// X coordinate axis.
		vertexData[valueCount++] = -0.9f;
		vertexData[valueCount++] = -0.8f;
		vertexData[valueCount++] = 0.9f;
		vertexData[valueCount++] = -0.8f;

		// Y coordinate axis.
		vertexData[valueCount++] = -0.8f;
		vertexData[valueCount++] = -0.9f;
		vertexData[valueCount++] = -0.8f;
		vertexData[valueCount++] = 0.9f;

		// X coordinate axis arrow.
		vertexData[valueCount++] = 0.85f;
		vertexData[valueCount++] = -0.75f;
		vertexData[valueCount++] = 0.85f;
		vertexData[valueCount++] = -0.85f;
		vertexData[valueCount++] =  0.9f;
		vertexData[valueCount++] = -0.8f;

		// Y coordinate axis arrow.
		vertexData[valueCount++] = -0.85f;
		vertexData[valueCount++] = 0.85f;
		vertexData[valueCount++] = -0.75f;
		vertexData[valueCount++] = 0.85f;
		vertexData[valueCount++] = -0.8f;
		vertexData[valueCount++] = 0.9f;
	}
	/**
	 * Resets the model to the identity matrix.
	 *
	 * @param e Action event
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "b": // Bottom
			setBottom(checkValue(textBottom, bottom, top, true));
			break;
		case "d": // Default (identity)
			textBottom.setText(Float.toString(model.getMinValue(plotSelection)));
			textLeft.setText(Float.toString(0.0f));
			textRight.setText(Float.toString(model.getMaxBin(plotSelection, 8)));
			textTop.setText(Float.toString(model.getMaxValue(plotSelection)));
			textCount.setText(Integer.toString(8));
			offsetX = 0.0f;
			update();
			break;
		case "l": // Left
			setLeft(checkValue(textLeft, left, right, true));
			break;
		case "r": // Right
			setRight(checkValue(textRight, right, left, false));
			break;
		case "t": // Top
			setTop(checkValue(textTop, top, bottom, false));
			break;
		case "c": // Count
			setCount(checkIntValue(textCount, count, 1000, true));
			break;
		}
		repaint();
	}

	/**
	 * Create a text field.
	 *
	 * @param d default value.
	 * @param c Command.
	 * @param l Action listener.
	 * @return Text field.
	 */
	private JTextField getTextField(float d, String c, ActionListener l) {
		JTextField t = new JTextField(Float.toString(d));
		t.setName(c);
		t.setActionCommand(c);
		t.addActionListener(l);
		t.addFocusListener(this);
		return t;
	}

	/**
	 * Get the left border value.
	 *
	 * @return Left border.
	 */
	public float getLeft() {
		return left;
	}

	/**
	 * Set the left border value.
	 * .
	 * @param l Left border.
	 */
	public void setLeft(float l) {
		left = l;
		update();
	}

	/**
	 * Get the right border value.
	 *
	 * @return Right border.
	 */
	public float getRight() {
		return right;
	}

	/**
	 * Set the right border value.
	 * .
	 * @param r Right border.
	 */
	public void setRight(float r) {
		right = r;
		update();
	}

	/**
	 * Get the bottom border value.
	 *
	 * @return Bottom border.
	 */
	public float getBottom() {
		return bottom;
	}

	/**
	 * Set the bottom border value.
	 * .
	 * @param b Bottom border.
	 */
	public void setBottom(float b) {
		bottom = b;
		update();
	}

	/**
	 * Get the top border value.
	 *
	 * @return Top border.
	 */
	public float getTop() {
		return top;
	}

	/**
	 * Set the top border value.
	 * .
	 * @param t Top border.
	 */
	public void setTop(float t) {
		top = t;
		update();
	}

	/**
	 * Set the number of bins value.
	 * .
	 * @param t Top border.
	 */
	public void setCount(float c) {
		count = Math.round(c);
		update();
	}

	/**
	 * Set the transformation.
	 */
	private void update() {
		left = Float.parseFloat(textLeft.getText());
		right = Float.parseFloat(textRight.getText());
		bottom = Float.parseFloat(textBottom.getText());
		top = Float.parseFloat(textTop.getText());
		count = Integer.parseInt(textCount.getText());
		model.setHistogramCurrent(plotSelection, left, right, bottom, top, count);
	}

	/**
	 * Check the value entered in the text field.
	 *
	 * @param t Text field.
	 * @param old The old value.
	 * @param limit The limit value.
	 * @param max Limit type (true - max, false - min).
	 * @return New value.
	 */
	private float checkValue(JTextField t, float old, float limit, boolean max) {
		float value = old;
		try {
			value = Float.parseFloat(t.getText());
			if ((max && value > limit) || (!max && value < limit)) {
				value = old;
			}
		}
		catch (NumberFormatException e) {
			value = old;
		}
		t.setText(Float.toString(value));
		return value;
	}
	/**
	 * Check the value entered in the text field.
	 *
	 * @param t Text field.
	 * @param old The old value.
	 * @param limit The limit value.
	 * @param max Limit type (true - max, false - min).
	 * @return New value.
	 */
	private float checkIntValue(JTextField t, int old, int limit, boolean max) {
		int value = old;
		try {
			value = Integer.parseInt(t.getText());
			if ((max && value > limit) || (!max && value < limit)) {
				value = old;
			}
		}
		catch (NumberFormatException e) {
			value = old;
		}
		t.setText(Integer.toString(value));
		return value;
	}

	/**
	 * Overridden as an empty method.
	 *
	 * @param e focus event.
	 */
	@Override
	public void focusGained(FocusEvent e) {	}

	/**
	 * Sets the value in the text field to the last stored value.
	 * Only pressing the RETURN key can change text field value.
	 *
	 * @param e focus event.
	 */
	@Override
	public void focusLost(FocusEvent e) {
		Object s = e.getSource();
		if (s == textLeft) {
			textLeft.setText(Float.toString(left));
		}
		else if (s == textRight) {
			textRight.setText(Float.toString(right));
		}
		else if (s == textBottom) {
			textBottom.setText(Float.toString(bottom));
		}
		else if (s == textTop) {
			textTop.setText(Float.toString(top));
		}
		else if (s == textCount) {
			textCount.setText(Integer.toString(count));
		}

	}

}
