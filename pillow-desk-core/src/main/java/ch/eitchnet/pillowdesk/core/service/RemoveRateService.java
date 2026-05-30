package ch.eitchnet.pillowdesk.core.service;

import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.TYPE_RATE;

public class RemoveRateService extends AbstractService<RemoveRateService.RemoveRateArg, ServiceResult> {

	public static class RemoveRateArg extends ServiceArgument {
		public String id;
		public RemoveRateArg() {}
		public RemoveRateArg(String id) { this.id = id; }
	}

	@Override
	protected ServiceResult internalDoService(RemoveRateArg arg) {
		if (arg.id == null)
			return ServiceResult.error("Rate ID is missing!");

		try (StrolchTransaction tx = openArgOrUserTx(arg)) {
			Resource rate = tx.getResourceBy(TYPE_RATE, arg.id);
			if (rate == null)
				return ServiceResult.error("Rate with ID " + arg.id + " does not exist!");

			tx.remove(rate);
			tx.commitOnClose();
		}

		return ServiceResult.success();
	}

	@Override
	protected ServiceResult getResultInstance() {
		return new ServiceResult();
	}

	@Override
	public RemoveRateArg getArgumentInstance() {
		return new RemoveRateArg();
	}
}
