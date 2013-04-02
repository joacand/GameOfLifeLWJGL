package GameOfLife;

import java.awt.Point;

import org.lwjgl.opengl.GL11;

public class Cell {
	
	private Point pos;
	private int adj;
	
	public Cell (int x, int y) {
		pos = new Point(x,y);
		adj = 0;
	}
	
	public void setPos(Point p) {
		pos = p;
	}
	
	public void setAdj(int a) {
		adj=a;
	}
	
	public Point getPos() {
		return pos;
	}
	
	public int getAdj() {
		return adj;
	}
	
	public void update(int w, int h) {
		GL11.glColor3f(0, 0, 0);
		GL11.glRectd(pos.x*w, pos.y*h, pos.x*w+w, pos.y*h+h);
	}

}

