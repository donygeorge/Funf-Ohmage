package edu.mit.media.funf.funfohmage;

public class SensorData {
	private long id;
	private String data;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	// Will be used by the ArrayAdapter in the ListView
	@Override
	public String toString() {
		return data	;
	}
}
