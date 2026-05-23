package ch.eitchnet.pillowdesk.core.service;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import li.strolch.model.Order;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

public class UpdateStayService extends AbstractService<UpdateStayService.UpdateStayArg, ServiceResult> {

    public static class UpdateStayArg extends ServiceArgument {
        public Order stay;
    }

    @Override
    protected ServiceResult internalDoService(UpdateStayArg arg) {
        if (arg.stay == null)
            return ServiceResult.error("Stay is missing!");

        try (StrolchTransaction tx = openArgOrUserTx(arg)) {
            if (!tx.hasOrder(arg.stay.getType(), arg.stay.getId()))
                return ServiceResult.error("Stay with ID " + arg.stay.getId() + " does not exist!");

            StayCalculatorPolicy.calculateAndFill(tx, arg.stay);
            tx.update(arg.stay);
            tx.commitOnClose();
        }

        return ServiceResult.success();
    }

    @Override
    protected ServiceResult getResultInstance() {
        return new ServiceResult();
    }

    @Override
    public UpdateStayArg getArgumentInstance() {
        return new UpdateStayArg();
    }
}
