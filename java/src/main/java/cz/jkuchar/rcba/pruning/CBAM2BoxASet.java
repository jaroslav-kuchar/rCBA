package cz.jkuchar.rcba.pruning;

import java.io.Serializable;

import cz.jkuchar.rcba.rules.Item;

public class CBAM2BoxASet implements Serializable {

	private static final long serialVersionUID = 1L;

	protected int did;
	protected String dClass;
	protected int cRule;
	protected int wRule;
	protected Item item;

	public CBAM2BoxASet(int did, String dClass, int cRule, int wRule, Item item) {
		super();
		this.did = did;
		this.dClass = dClass;
		this.cRule = cRule;
		this.wRule = wRule;
		this.item = item;
	}

}
