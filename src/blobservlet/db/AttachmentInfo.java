package blobservlet.db;

public class AttachmentInfo {
	private String name;
	private String key;
	
	public AttachmentInfo( String name, String key ) {
		this.name = name;
		this.key = key;
	}
	
	public boolean equals( AttachmentInfo comp ) {
		return this.name.equals(comp.name);
	}
	
	public String toString() {
		return name;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
}