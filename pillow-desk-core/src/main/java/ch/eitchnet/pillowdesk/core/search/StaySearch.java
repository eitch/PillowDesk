package ch.eitchnet.pillowdesk.core.search;

import li.strolch.search.OrderSearch;

import java.time.ZonedDateTime;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class StaySearch extends OrderSearch {

	public StaySearch() {
		super();
		types(TYPE_STAY);
	}

	public StaySearch guestName(String name) {
		if (name != null && !name.isEmpty()) {
			where(param(PARAM_GUEST_NAME).containsIgnoreCase(name));
		}
		return this;
	}

	public StaySearch dateRange(ZonedDateTime fromZdt, ZonedDateTime toZdt) {
		if (fromZdt != null && toZdt != null) {

			// A stay overlaps if checkIn < to AND checkOut > from
			where(param(PARAM_CHECK_IN).isBefore(toZdt, false).and(param(PARAM_CHECK_OUT).isAfter(fromZdt, false)));
		}
		return this;
	}

	public StaySearch isAirBnb(Boolean isAirBnb) {
		if (isAirBnb != null) {
			where(param(PARAM_IS_AIR_BNB).isEqualTo(isAirBnb));
		}
		return this;
	}

	public StaySearch bookingId(String bookingId) {
		if (bookingId != null && !bookingId.isEmpty()) {
			where(param(PARAM_BOOKING_ID).isEqualTo(bookingId));
		}
		return this;
	}

	public StaySearch room(String roomId) {
		if (roomId != null && !roomId.isEmpty()) {
			where(param(BAG_RELATIONS, PARAM_ROOM).isEqualTo(roomId));
		}
		return this;
	}
}
