package cz.jkuchar.rcba.pruning;

public class ASet {
	int did;
	String dclass;
	int cRule;
	int wRule;

	public ASet(int did, String dclass, int cRule, int wRule) {
		super();
		this.did = did;
		this.dclass = dclass;
		this.cRule = cRule;
		this.wRule = wRule;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cRule;
		result = prime * result + ((dclass == null) ? 0 : dclass.hashCode());
		result = prime * result + did;
		result = prime * result + wRule;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ASet other = (ASet) obj;
		if (cRule != other.cRule)
			return false;
		if (dclass == null) {
			if (other.dclass != null)
				return false;
		} else if (!dclass.equals(other.dclass))
			return false;
		if (did != other.did)
			return false;
		if (wRule != other.wRule)
			return false;
		return true;
	}

}