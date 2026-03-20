package org.kyc.digilockerService.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDate {
  public static LocalDate generateRandomDOB() {
    LocalDate start = LocalDate.of(1900, 1, 1);
    LocalDate end = LocalDate.now().minusYears(18);
    long days = ChronoUnit.DAYS.between(start, end);
    long randomDays = ThreadLocalRandom.current().nextLong(days + 1);
    return start.plusDays(randomDays);
  }
}
