package GameOfLife;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glNewList;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glVertex2d;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.Timer;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public class GameOfLife implements ActionListener {

	private int fps;
	private long lastFPS;
	private int rows;
	private int cols;
	private int rowH;
	private int rowW;
	private final HashMap<Point,Cell> cellD = new HashMap<Point,Cell>();
	private boolean gameOn;
	private boolean busy=false;
	private boolean renderBusy=false;
	private int SPEED = 50;
	private final Timer timer;
	private int numOfTicks;
	private int displayListHandle;

	public GameOfLife() {
		timer = new Timer(SPEED,this);
		lastFPS = getTime();

		initDisplay();
		initOpenGL();

		createGrid(80,100);

		// Without this unnecessary cell the window won't render before mouse click
		cellD.put(new Point(-1,-1), new Cell(-1, -1));

		while (!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT);  

			updateFPS();

			if (gameOn) {
				timer.start();
			}

			pollKeyboard();
			if (!busy) {
				render();
			}

			Display.update();
			Display.sync(60);
		}
		Display.destroy();
		System.exit(0);
	}

	private void createGameFromFile(String file) {
		if (!renderBusy && !busy) {
			renderBusy=true;
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String content;

				while ((content = br.readLine()) != null) {
					String[] split = content.split(" ");
					Point newP = new Point(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
					if (!cellD.containsKey(newP)) {
						cellD.put(newP, new Cell(newP.x,newP.y));
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				System.out.println("ERROR: File not found");
			} catch (IOException e) {
				e.printStackTrace();
			}
			renderBusy=false;
		}
	}

	private void resetGame() {
		timer.stop();
		System.out.println("resetting");
		gameOn=false;
		cellD.clear();
		numOfTicks=0;
	}

	private void pollKeyboard() {
		// Mouse polling
		if (!busy) {
			if (Mouse.isButtonDown(0)) {
				int row=(-Mouse.getY())/(Display.getHeight()/rows);
				row+=rows-1;
				final int col=Mouse.getX()/(Display.getWidth()/cols);

				final Point posClicked = new Point(col,row);

				if (!cellD.containsKey(posClicked)) {
					cellD.put(posClicked, new Cell(posClicked.x, posClicked.y));
				}	            
				System.out.println("Clicked " + row + ", " + col);
			} else if (Mouse.isButtonDown(1)) {
				int row=(-Mouse.getY())/(Display.getHeight()/rows);
				row+=rows-1;
				final int col=Mouse.getX()/(Display.getWidth()/cols);

				final Point posClicked = new Point(col,row);

				if (cellD.containsKey(posClicked)) {
					cellD.remove(posClicked);
				}	            
			} 
		}

		// Keyboard polling
		while (Keyboard.next()) {
			if (Keyboard.getEventKey() == Keyboard.KEY_RETURN && Keyboard.getEventKeyState()) {
				gameOn=true;
			} else if (Keyboard.getEventKey() == Keyboard.KEY_SPACE && Keyboard.getEventKeyState()) {
				resetGame();
			} else if (Keyboard.getEventKey() == Keyboard.KEY_UP && Keyboard.getEventKeyState()) {
				if (SPEED > 11) {
					SPEED-=10;
					timer.setDelay(SPEED);
				}
			} else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN && Keyboard.getEventKeyState()) {
				SPEED+=10;
				timer.setDelay(SPEED);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_1 && Keyboard.getEventKeyState()) {
				createGameFromFile("res/gun.txt");
			} else if (Keyboard.getEventKey() == Keyboard.KEY_2 && Keyboard.getEventKeyState()) {
				createGameFromFile("res/infsmall.txt");
			} else if (Keyboard.getEventKey() == Keyboard.KEY_3 && Keyboard.getEventKeyState()) {
				createGameFromFile("res/start.txt");
			}
		}

	}

	private void render() {
		// Render the grid with display lists
		glCallList(displayListHandle);

		renderBusy=true;
		// Render the cells with immediate rendering
		for (final Entry<Point, Cell> e : cellD.entrySet()) {
			e.getValue().update(rowW, rowH);
		}
		renderBusy=false;
	}

	private void createGrid(final int r, final int c) {
		rows = r;
		cols = c;
		final int rows = r;
		final int cols = c;
		rowH = Display.getHeight()/rows;
		rowW = Display.getWidth()/cols;

		displayListHandle = glGenLists(1);

		glNewList(displayListHandle, GL_COMPILE);

		for (int i = 0; i*rowH < Display.getHeight(); i++) {
			glBegin(GL_LINES);
			glVertex2d(0, i*rowH);
			glVertex2d(Display.getWidth(), i*rowH);
			glEnd();
		}

		for (int i = 0; i*rowW < Display.getWidth(); i++) {
			glBegin(GL_LINES);
			glVertex2d(i*rowW, 0);
			glVertex2d(i*rowW, Display.getHeight());
			glEnd();
		}

		glEndList();
	}

	public void initOpenGL() {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, Display.getWidth(), Display.getHeight(), 0, 1, -1);
		glMatrixMode(GL_MODELVIEW);
	}

	public void initDisplay() {
		try {
			Display.setDisplayMode(new DisplayMode(1000,800));
			Display.setInitialBackground(1, 1, 1);
			Display.setTitle("Game of Life - FPS: " + fps + " - Ticks: " + numOfTicks + " - Speed: " + SPEED + "ms before tick");
			Display.create();
		} catch (final LWJGLException e) {
			System.out.println("Couldn't load display");
			Display.destroy();
			System.exit(1);
		}
	}

	public long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	public void updateFPS() {
		if (getTime() - lastFPS > 1000) {
			Display.setTitle("Game of Life - FPS: " + fps + " - Ticks: " + numOfTicks + " - Speed: " + SPEED + "ms before tick");
			fps = 0; //reset the FPS counter
			lastFPS += 1000; //add one second
		}
		fps++;
	}

	public List<Cell> fetchAdj(final Cell c) {
		final List<Cell> tmpCellList=new ArrayList<Cell>();

		final Point[] adj=new Point[] { 
				new Point(c.getPos().x-1, c.getPos().y-1),
				new Point(c.getPos().x, c.getPos().y-1),
				new Point(c.getPos().x+1, c.getPos().y-1),
				new Point(c.getPos().x-1, c.getPos().y),
				new Point(c.getPos().x+1, c.getPos().y),
				new Point(c.getPos().x-1, c.getPos().y+1),
				new Point(c.getPos().x, c.getPos().y+1),
				new Point(c.getPos().x+1, c.getPos().y+1)
		};

		for (int i=0; i<adj.length; i++) {
			if (!cellD.containsKey(adj[i])) {
				tmpCellList.add(new Cell(adj[i].x, adj[i].y));
			}
		}

		return tmpCellList;
	}

	public int getAdj(final Cell c) {
		int adj=0;

		final Point[] posArr=new Point[] { 
				new Point(c.getPos().x-1, c.getPos().y-1),
				new Point(c.getPos().x, c.getPos().y-1),
				new Point(c.getPos().x+1, c.getPos().y-1),
				new Point(c.getPos().x-1, c.getPos().y),
				new Point(c.getPos().x+1, c.getPos().y),
				new Point(c.getPos().x-1, c.getPos().y+1),
				new Point(c.getPos().x, c.getPos().y+1),
				new Point(c.getPos().x+1, c.getPos().y+1)
		};

		for (final Point p : posArr) {
			if (p.x<1||p.y<1||p.x>Display.getWidth()||p.y>Display.getHeight()) {
				p.x=-1;
				p.y=-1;
			}
		}

		for (int i =0; i < posArr.length; i++) {
			Cell value=null;

			if (cellD.containsKey(posArr[i])) {
				value = cellD.get(posArr[i]);
			}
			if (value != null) {
				adj++;
			}
		}

		return adj;

	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (!renderBusy) {			
			busy=true;
			numOfTicks++;
			final List<Cell> aboutToAdd = new ArrayList<Cell>();
			final List<Cell> aboutToRemove = new ArrayList<Cell>();

			for (final Entry<Point, Cell> c : cellD.entrySet()) {
				if (!(c.getValue().getPos().x==-1||c.getValue().getPos().y==-1)) {
					//
					final List<Cell> adj=fetchAdj(c.getValue());

					for (final Cell ce : adj) {
						if (getAdj(ce)==3&&!aboutToAdd.contains(ce)) {
							aboutToAdd.add(ce);
						}
					}

					switch (getAdj(c.getValue())) {
					case 0:
					case 1:
						aboutToRemove.add(c.getValue());
						break;
					case 2:
					case 3:
						break;
					default:
						aboutToRemove.add(c.getValue());
						break;
					}
				}
			}

			for (final Cell ce : aboutToAdd) {
				if (!cellD.containsKey(ce.getPos())) {
					cellD.put(ce.getPos(), ce);
				}
			}

			for (final Cell ce : aboutToRemove) {
				cellD.remove(ce.getPos());
			}
			busy=false;
		}
	}
}
