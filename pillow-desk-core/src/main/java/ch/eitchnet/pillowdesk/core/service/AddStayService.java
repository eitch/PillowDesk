package ch.eitchnet.pillowdesk.core.service;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import li.strolch.model.Order;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.service.api.AbstractService;
import li.strolch.service.api.ServiceArgument;
import li.strolch.service.api.ServiceResult;

public class AddStayService extends AbstractService<AddStayService.AddStayArg, ServiceResult> {

    public static class AddStayArg extends ServiceArgument {
        public Order stay;
    }

    @Override
    protected ServiceResult internalDoService(AddStayArg arg) {
        if (arg.stay == null)
            return ServiceResult.error("Stay is missing!");

        try (StrolchTransaction tx = openArgOrUserTx(arg)) {
            if (tx.hasOrder(arg.stay.getType(), arg.stay.getId()))
                return ServiceResult.error("Stay with ID " + arg.stay.getId() + " already exists!");

            StayCalculatorPolicy.calculateAndFill(tx, arg.stay);
            tx.add(arg.stay);
            tx.commitOnClose();
        }

        return ServiceResult.success();
    }

    @Override
    protected ServiceResult getResultInstance() {
        return new ServiceResult();
    }

    @Override
    public AddStayArg getArgumentInstance() {
        return new AddStayArg();
    }
}
