package com.rkoyanagui;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rkoyanagui.core.OrElseFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;

class ElsetilityTest
{
  @Test
  void succeed()
  {
    final OrElseFactory factory = Elsetility.await().timeout(ofSeconds(1));
    assertDoesNotThrow(() -> factory.until(() -> true));
  }

  @Test
  void fail()
  {
    final OrElseFactory factory = Elsetility.await().timeout(ofSeconds(1));
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
  }

  @Test
  void match()
  {
    final OrElseFactory factory = Elsetility.await().timeout(ofSeconds(1));
    assertDoesNotThrow(() -> factory.until(() -> "", emptyString()));
  }

  @Test
  void mismatch()
  {
    final OrElseFactory factory = Elsetility.await().timeout(ofSeconds(1));
    assertThrows(ConditionTimeoutException.class,
        () -> factory.until(() -> "", not(emptyString())));
  }

  @Test
  void succeedPredicate()
  {
    final OrElseFactory factory = Elsetility.await().timeout(ofSeconds(1));
    assertDoesNotThrow(() -> factory.until(() -> "", s -> s.isEmpty()));
  }

  @Test
  void failPredicate()
  {
    final OrElseFactory factory = Elsetility.await().timeout(ofSeconds(1));
    assertThrows(ConditionTimeoutException.class,
        () -> factory.until(() -> "", s -> !s.isEmpty()));
  }

  @Test
  void callOrElseDoAndReachMaxAttempts()
  {
    final AtomicInteger i = new AtomicInteger();
    final OrElseFactory factory = Elsetility.await()
        .timeout(ofSeconds(10))
        .maxNumOfAttempts(3)
        .orElseDo(() -> i.incrementAndGet());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
    assertThat(i.get(), is(equalTo(2)));
  }

  @Test
  void doNotCallOrElseDoIfOnlyOneAttempt()
  {
    final AtomicInteger i = new AtomicInteger();
    final OrElseFactory factory = Elsetility.await()
        .timeout(ofSeconds(10))
        .maxNumOfAttempts(1)
        .orElseDo(() -> i.incrementAndGet());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
    assertThat(i.get(), is(equalTo(0)));
  }

  @Test
  void callOrElseDoAndExpireTimeout()
  {
    final AtomicInteger i = new AtomicInteger();
    final OrElseFactory factory = Elsetility.await()
        .timeout(ofSeconds(5))
        .maxNumOfAttempts(10)
        .orElseDo(() -> i.incrementAndGet());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
    assertThat(i.get(), is(greaterThan(0)));
  }

  @Test
  void ignoreExceptionInSupplier()
  {
    final OrElseFactory factory = Elsetility.await()
        .maxNumOfAttempts(1)
        .timeout(ofSeconds(10))
        .ignoreExceptionsMatching(instanceOf(ArithmeticException.class));
    assertThrows(ConditionTimeoutException.class,
        () -> factory.until(() -> 1 / 0, is(equalTo(0))));
  }

  @Test
  void doNotIgnoreExceptionInSupplier()
  {
    final OrElseFactory factory = Elsetility.await().maxNumOfAttempts(1);
    assertThrows(ArithmeticException.class,
        () -> factory.until(() -> 1 / 0, is(equalTo(0))));
  }

  @Test
  void ignoreExceptionInOrElseDo()
  {
    final OrElseFactory factory = Elsetility.await()
        .ignoreExceptionsMatching(instanceOf(ArithmeticException.class))
        .orElseDo(() ->
        {
          int i = 1 / 0;
        });
    assertThrows(ConditionTimeoutException.class,
        () -> factory.until(() -> 0, is(equalTo(1))));
  }

  @Test
  void doNotIgnoreExceptionInOrElseDo()
  {
    final OrElseFactory factory = Elsetility.await()
        .orElseDo(() ->
        {
          int i = 1 / 0;
        });
    assertThrows(ArithmeticException.class,
        () -> factory.until(() -> 0, is(equalTo(1))));
  }

  @Test
  void ignoreExceptionInCondition()
  {
    final OrElseFactory factory = Elsetility.await()
        .maxNumOfAttempts(1)
        .timeout(ofSeconds(10))
        .ignoreExceptionsMatching(instanceOf(ArithmeticException.class));
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> 1 / 0 == 0));
  }

  @Test
  void doNotIgnoreExceptionInCondition()
  {
    final OrElseFactory factory = Elsetility.await();
    assertThrows(ArithmeticException.class, () -> factory.until(() -> 1 / 0 == 0));
  }
}
