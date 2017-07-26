package blobservlet.db;

import java.sql.Blob;

public class BlobSet {
	private String name;
	private String path;
	private int expKey;
	private Blob blob;
	
	public BlobSet( String name, String path, Blob blob, int expKey ) {
		this.setName(name); this.path = path; this.setBlob(blob); this.expKey = expKey;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Blob getBlob() {
		return blob;
	}

	public void setBlob(Blob blob) {
		this.blob = blob;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getExpKey() {
		return expKey;
	}

	public void setExpKey(int expKey) {
		this.expKey = expKey;
	}
}
