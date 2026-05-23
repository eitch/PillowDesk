package ch.eitchnet.pillowdesk.rest;

import li.strolch.rest.StrolchRestfulClasses;

import java.util.HashSet;
import java.util.Set;

public class PillowDeskRestfulClasses {

    public static Set<Class<?>> getRestfulClasses() {
        Set<Class<?>> restfulClasses = new HashSet<>(StrolchRestfulClasses.getRestfulClasses());
        restfulClasses.add(StayResource.class);
        restfulClasses.add(SummaryResource.class);
        restfulClasses.add(RoomResource.class);
        restfulClasses.add(RateResource.class);
        return restfulClasses;
    }

    public static Set<Class<?>> getProviderClasses() {
        return StrolchRestfulClasses.getProviderClasses();
    }
}
