package ch.eitchnet.pillowdesk.core.search;

import li.strolch.search.ResourceSearch;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.TYPE_RATE;

public class RateSearch extends ResourceSearch {

	public RateSearch() {
		super();
		types(TYPE_RATE);
	}

	public RateSearch name(String name) {
		if (name != null && !name.isEmpty()) {
			where(name().containsIgnoreCase(name));
		}
		return this;
	}
}
