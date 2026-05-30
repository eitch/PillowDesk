package ch.eitchnet.pillowdesk.core.search;

import li.strolch.search.ResourceSearch;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class RateOverrideSearch extends ResourceSearch {

	public RateOverrideSearch() {
		super();
		types(TYPE_RATE_OVERRIDE);
	}

	public RateOverrideSearch name(String name) {
		if (name != null && !name.isEmpty()) {
			where(name().containsIgnoreCase(name));
		}
		return this;
	}

	public RateOverrideSearch forRate(String rateId) {
		where(param(BAG_RELATIONS, PARAM_RATE, isEqualTo(rateId)));
		return this;
	}
}
