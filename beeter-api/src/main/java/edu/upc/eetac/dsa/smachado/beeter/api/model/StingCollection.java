package edu.upc.eetac.dsa.smachado.beeter.api.model;

import java.util.ArrayList;
import java.util.List;

public class StingCollection {
	private List<Sting> stings;

	public StingCollection() {
		super();
		stings = new ArrayList<>();
	}

	public List<Sting> getStings() {
		return stings;
	}

	public void setStings(List<Sting> stings) {
		this.stings = stings;
	}

	public void addSting(Sting sting) {
		stings.add(sting);
	}
}