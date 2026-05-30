package ch.eitchnet.pillowdesk.core.service;

import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.StringArgument;
import li.strolch.service.api.ServiceResult;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class RemoveRateOverrideService extends AbstractService<StringArgument, ServiceResult> {

	@Override
	protected ServiceResult internalDoService(StringArgument arg) {
		if (arg.value == null || arg.value.isEmpty())
			return ServiceResult.error("RateOverride ID is missing!");

		try (StrolchTransaction tx = openArgOrUserTx(arg)) {
			Resource rateOverride = tx.getResourceBy(TYPE_RATE_OVERRIDE, arg.value, true);
			tx.remove(rateOverride);
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
	public StringArgument getArgumentInstance() {
		return new StringArgument();
	}
}
