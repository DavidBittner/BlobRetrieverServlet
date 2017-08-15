package blobservlet.db;

import java.util.ArrayList;

public class Tree {
	String title;
	String past;
	String key;
	
	boolean isLeaf;
	boolean folder;
	int depth;
	ArrayList<Tree> children;
	
	public Tree( String name, String key, Tree parent ) {
		children = new ArrayList<>();
		isLeaf = true;
		folder = !isLeaf;
		
		if( parent == null ) {
			depth = 0;
		}else{
			depth = parent.depth+1;
		}
		
		this.title = name;
		this.key = key;
		
		if( folder ) {
			this.key = "";
		}
	}
	
	public Tree( String name, Tree child )
	{
		children = new ArrayList<>();
		isLeaf = false;
		depth = 0;
		folder = !isLeaf;
		
		this.title = name;
		this.key = "1";
		
		add(child);
	}
	
	public void add( Tree newChild ) {
		children.add(newChild);
		isLeaf = false;
		folder = !isLeaf;
	}
	
	public void add( ArrayList<Tree> newChildren ) {
		for( Tree i : newChildren ) {
			children.add( i );
		}
		isLeaf = false;
		folder = !isLeaf;
		
		if( folder ) {
			this.key = "";
		}
	}
	
	public String toString() {
		return title;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public String getTitle() {
		return title;
	}
	
	public ArrayList<Tree> getChildren() {
		return children;
	}
	
	public boolean isLeaf() {
		return isLeaf;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
}
