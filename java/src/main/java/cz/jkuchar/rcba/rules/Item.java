package cz.jkuchar.rcba.rules;

import java.util.List;

public class Item {

	private TupleCollection memory;
	private long id;

	public Item() {
		this(0);
	}

	public Item(int id) {
		this.memory = new TupleCollection();
		this.id = id;
	}

	public List<String> get(String key) {
		return this.memory.get(key);
	}

	public void put(String key, String value) {
		this.memory.put(key, value);
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Item [memory=" + memory + "]";
	}

}
