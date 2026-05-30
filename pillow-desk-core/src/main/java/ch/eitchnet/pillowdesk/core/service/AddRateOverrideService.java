package ch.eitchnet.pillowdesk.core.service;

import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class AddRateOverrideService extends AbstractService<AddRateOverrideService.AddRateOverrideArg, ServiceResult> {

	public static class AddRateOverrideArg extends ServiceArgument {
		public Resource rateOverride;
	}

	@Override
	protected ServiceResult internalDoService(AddRateOverrideArg arg) {
		if (arg.rateOverride == null)
			return ServiceResult.error("RateOverride is missing!");
		if (!arg.rateOverride.getType().equals(TYPE_RATE_OVERRIDE))
			return ServiceResult.error("Invalid type: " + arg.rateOverride.getType());

		try (StrolchTransaction tx = openArgOrUserTx(arg)) {
			tx.add(arg.rateOverride);
			tx.commitOnClose();
		} catch (Exception e) {
			return ServiceResult.error(e.getMessage());
		}

		return ServiceResult.success();
	}

	@Override
	protected ServiceResult getResultInstance() {
		return new ServiceResult();
	}

	@Override
	public AddRateOverrideArg getArgumentInstance() {
		return new AddRateOverrideArg();
	}
}
