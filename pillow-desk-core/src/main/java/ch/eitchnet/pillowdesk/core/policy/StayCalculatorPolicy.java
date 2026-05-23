package ch.eitchnet.pillowdesk.core.policy;

import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.parameter.*;
import li.strolch.persistence.api.StrolchTransaction;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class StayCalculatorPolicy {

    public record StayCosts(
            long nights,
            double touristTax,
            double accommodationGross,
            double accommodationNet,
            double totalGross
    ) {}

    public static StayCosts calculate(Order stay, Resource rate) {
        DateParameter checkInParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN);
        ZonedDateTime checkIn = checkInParam.getValueZdt();
        DateParameter checkOutParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT);
        ZonedDateTime checkOut = checkOutParam.getValueZdt();
        
        IntegerParameter adultsParam = stay.getParameter(BAG_PARAMETERS, PARAM_ADULTS, false);
        int adults = adultsParam == null ? 2 : adultsParam.getValue();
        IntegerParameter childrenParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHILDREN, false);
        int children = childrenParam == null ? 0 : childrenParam.getValue();

        FloatParameter basePriceParam = rate.getParameter(BAG_PARAMETERS, PARAM_BASE_PRICE);
        double basePrice = basePriceParam.getValue();
        FloatParameter serviceFeeParam = rate.getParameter(BAG_PARAMETERS, PARAM_SERVICE_FEE);
        double serviceFee = serviceFeeParam.getValue();
        FloatParameter discountParam = rate.getParameter(BAG_PARAMETERS, PARAM_DISCOUNT);
        double discount = discountParam.getValue();

        long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
        if (nights < 0) nights = 0;

        double touristTax = nights * (2.0 * adults + 1.0 * children);
        double accommodationGross = nights * basePrice * (1.0 - discount);
        double accommodationNet = accommodationGross * (1.0 - serviceFee);
        double totalGross = accommodationGross + touristTax;

        return new StayCosts(nights, touristTax, accommodationGross, accommodationNet, totalGross);
    }

    public static void calculateAndFill(StrolchTransaction tx, Order stay) {
        Resource rate = tx.getResourceByRelation(stay, PARAM_RATE, true);
        StayCosts costs = calculate(stay, rate);

        FloatParameter totalRevenueParam = stay.getParameter(BAG_PARAMETERS, PARAM_TOTAL_REVENUE, false);
        if (totalRevenueParam == null) {
            totalRevenueParam = new FloatParameter(PARAM_TOTAL_REVENUE, "Total Revenue", costs.totalGross());
            stay.addParameter(BAG_PARAMETERS, totalRevenueParam);
        } else {
            totalRevenueParam.setValue(costs.totalGross());
        }
    }
}
