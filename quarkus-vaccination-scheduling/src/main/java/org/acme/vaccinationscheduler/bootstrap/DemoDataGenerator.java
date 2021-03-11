/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.vaccinationscheduler.bootstrap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.acme.vaccinationscheduler.domain.Location;
import org.acme.vaccinationscheduler.domain.Person;
import org.acme.vaccinationscheduler.domain.Timeslot;
import org.acme.vaccinationscheduler.domain.VaccinationCenter;
import org.acme.vaccinationscheduler.domain.VaccinationSchedule;
import org.acme.vaccinationscheduler.domain.VaccinationSlot;
import org.acme.vaccinationscheduler.domain.VaccineType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.YEARS;

public class DemoDataGenerator {

    public static final String[] PERSON_FIRST_NAMES = {
            "Ann", "Beth", "Carl", "Dan", "Elsa", "Flo", "Gus", "Hugo", "Ivy", "Jay",
            "Kurt", "Luke", "Mia", "Noa", "Otto", "Paul", "Quin", "Ray", "Sue", "Taj",
            "Uma", "Vix", "Wade", "Xiu" , "Yuna", "Zara"};
    public static final String[] VACCINATION_CENTER_NAMES = {
            "Downtown", "Uptown", "Market", "Park", "River", "Square", "Bay", "Hill", "Station", "Hospital",
            "Tower", "Wall", "Plaza", "Grove", "Boulevard", "Avenue", "Fort", "Beach", "Harbor", "Airport",
            "Garden", "Forest", "Springs", "Ville" , "Stad"};
    public static double massVaccinationCenterRatio = (1.0 / 3.0);

    public static final LocalDate MINIMUM_BIRTH_DATE = LocalDate.of(1930, 1, 1);
    public static final int BIRTH_DATE_RANGE_LENGTH = (int) DAYS.between(MINIMUM_BIRTH_DATE, LocalDate.of(2000, 1, 1));

    protected static final Logger logger = LoggerFactory.getLogger(DemoDataGenerator.class);

    int vaccinationCenterCount;
    int totalLineCount;

    // Default latitude and longitude window: city of Atlanta, US.
    double minimumLatitude = 33.4;
    double maximumLatitude = 34.10;
    double minimumLongitude = -84.9;
    double maximumLongitude = -83.9;

    public void setVaccinationCenterCount(int vaccinationCenterCount) {
        this.vaccinationCenterCount = vaccinationCenterCount;
    }

    public void setTotalLineCount(int totalLineCount) {
        this.totalLineCount = totalLineCount;
    }

    public VaccinationSchedule createVaccinationSchedule() {
        List<VaccineType> vaccineTypeList = Arrays.asList(
                new VaccineType("Pfizer", 19, 21),
                new VaccineType("Moderna", 26, 28),
                new VaccineType("AstraZeneca", 4 * 7, 6 * 7, 55)
        );

        LocalDate windowStartDate = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        int windowDaysLength = 5;
        LocalTime dayStartTime = LocalTime.of(9, 0);
        int injectionsPerLinePerTimeslot = 4;
        int timeslotsPerLinePerDay = (int) HOURS.between(dayStartTime, LocalTime.of(17, 0));

        Random random = new Random(17);
        List<VaccinationCenter> vaccinationCenterList = new ArrayList<>(vaccinationCenterCount);
        int massCount = (int) Math.round(massVaccinationCenterRatio * vaccinationCenterCount);
        int massExtraLineCount = totalLineCount - vaccinationCenterCount;
        long vaccinationCenterId = 0L;
        for (int i = 0; i < vaccinationCenterCount; i++) {
            String name = VACCINATION_CENTER_NAMES[i % VACCINATION_CENTER_NAMES.length]
                    + (i < VACCINATION_CENTER_NAMES.length ? "" : " " + (i / VACCINATION_CENTER_NAMES.length + 1));
            int lineCount;
            if (i < massCount) {
                // The + i distributes the remainder, for example if massExtraLineCount=8 and massCount=3
                lineCount = 1 + (massExtraLineCount + i) / massCount;
            } else {
                lineCount = 1;
            }
            vaccinationCenterList.add(new VaccinationCenter(vaccinationCenterId++, name, pickLocation(random),
                    lineCount, injectionsPerLinePerTimeslot));
        }

        List<Timeslot> timeslotList = new ArrayList<>(windowDaysLength * timeslotsPerLinePerDay * totalLineCount);
        for (int dayIndex = 0; dayIndex < windowDaysLength; dayIndex++) {
            LocalDate date = windowStartDate.plusDays(dayIndex);
            for (int timeslotIndex = 0; timeslotIndex < timeslotsPerLinePerDay; timeslotIndex++) {
                timeslotList.add(new Timeslot(date,
                        dayStartTime.plusHours(timeslotIndex), dayStartTime.plusHours(timeslotIndex + 1)));
            }
        }

        List<VaccinationSlot> vaccinationSlotList = new ArrayList<>();
        long vaccinationSlotId = 0L;
        for (int dayIndex = 0; dayIndex < windowDaysLength; dayIndex++) {
            LocalDate date = windowStartDate.plusDays(dayIndex);
            List<Timeslot> dayTimeslotList = timeslotList.stream()
                    .filter(timeslot -> timeslot.getDate().equals(date))
                    .collect(Collectors.toList());
            for (VaccinationCenter vaccinationCenter : vaccinationCenterList) {
                Map<VaccineType, Integer> vaccineTypeToLineCountMap = new HashMap<>(vaccineTypeList.size());
                for (int lineIndex = 0; lineIndex < vaccinationCenter.getLineCount(); lineIndex++) {
                    VaccineType vaccineType = pickVaccineType(vaccineTypeList, random, null);
                    int lineCount = vaccineTypeToLineCountMap.computeIfAbsent(vaccineType, key -> 0);
                    lineCount++;
                    vaccineTypeToLineCountMap.put(vaccineType, lineCount);
                }
                for (Timeslot timeslot : dayTimeslotList) {
                    int lineIndexOffset = 0;
                    for (Map.Entry<VaccineType, Integer> entry : vaccineTypeToLineCountMap.entrySet()) {
                        VaccineType vaccineType = entry.getKey();
                        int lineCount = entry.getValue();
                        int capacity = vaccinationCenter.getInjectionsPerLinePerTimeslot() * lineCount;
                        VaccinationSlot vaccinationSlot = new VaccinationSlot(
                                vaccinationSlotId++, vaccinationCenter, timeslot,
                                vaccineType, lineIndexOffset, lineCount, capacity);
                        lineIndexOffset += lineCount;
                        vaccinationSlotList.add(vaccinationSlot);
                    }
                }
            }
        }

        int personListSize = (totalLineCount * timeslotList.size() * injectionsPerLinePerTimeslot) * 5 / 4; // 25% too many
        List<Person> personList = new ArrayList<>(personListSize);
        long personId = 0L;
        for (int i = 0; i < personListSize; i++) {
            int lastNameI = i / PERSON_FIRST_NAMES.length;
            String name = PERSON_FIRST_NAMES[i % PERSON_FIRST_NAMES.length]
                    + " " + (lastNameI < 26 ? ((char) ('A' + lastNameI)) + "." : lastNameI - 25);
            Location location = pickLocation(random);
            LocalDate birthdate = MINIMUM_BIRTH_DATE.plusDays(random.nextInt(BIRTH_DATE_RANGE_LENGTH));
            int age = (int) YEARS.between(birthdate, windowStartDate);
            boolean firstDoseInjected = random.nextDouble() < 0.25;
            VaccineType firstDoseVaccineType = firstDoseInjected ? pickVaccineType(vaccineTypeList, random, age) : null;
            LocalDate secondDoseIdealDate = firstDoseInjected ?
                    windowStartDate.plusDays(random.nextInt(windowDaysLength))
                    : null;
            LocalDate firstDoseDate = firstDoseInjected ?
                    secondDoseIdealDate.minusDays(firstDoseVaccineType.getSecondDoseIdealDays())
                    : null;
            Person person = new Person(personId++, name, location,
                    birthdate, age, firstDoseInjected, firstDoseVaccineType, firstDoseDate);
            personList.add(person);
        }
        return new VaccinationSchedule(vaccineTypeList, vaccinationCenterList,
                timeslotList, vaccinationSlotList, personList);
    }

    public Location pickLocation(Random random) {
        double latitude = minimumLatitude + (random.nextDouble() * (maximumLatitude - minimumLatitude));
        double longitude = minimumLongitude + (random.nextDouble() * (maximumLongitude - minimumLongitude));
        return new Location(latitude, longitude);
    }

    public VaccineType pickVaccineType(List<VaccineType> vaccineTypeList, Random random, Integer age) {
        List<VaccineType> suitableVaccineTypeList;
        if (age == null) {
            suitableVaccineTypeList = vaccineTypeList;
        } else {
            suitableVaccineTypeList = vaccineTypeList.stream()
                    .filter(vaccineType -> vaccineType.isOkForMaximumAge(age)).collect(Collectors.toList());
        }
        return suitableVaccineTypeList.get(random.nextInt(suitableVaccineTypeList.size()));
    }

}
