package com.rkoyanagui;

import static com.rkoyanagui.Elsetility.instanceOfAnyOf;
import static java.time.Duration.ofSeconds;
import static org.awaitility.core.ConditionEvaluationLogger.conditionEvaluationLogger;
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
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ElsetilityTest
{
  private static final Logger LOG = LoggerFactory.getLogger(ElsetilityTest.class);

  @BeforeAll
  static void setup()
  {
    Elsetility.setDefaultConditionEvaluationListener(
        conditionEvaluationLogger(s -> LOG.debug(s))
    );
  }

  @Test
  void succeed()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    assertDoesNotThrow(() -> factory.until(() -> true));
  }

  @Test
  void fail()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
  }

  @Test
  void match()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    assertDoesNotThrow(() -> factory.until(() -> "", emptyString()));
  }

  @Test
  void mismatch()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    final Matcher<String> matcher = not(emptyString());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> "", matcher));
  }

  @Test
  void succeedPredicate()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    assertDoesNotThrow(() -> factory.until(() -> "", s -> s.isEmpty()));
  }

  @Test
  void failPredicate()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    assertThrows(ConditionTimeoutException.class,
        () -> factory.until(() -> "", s -> !s.isEmpty()));
  }

  @Test
  void call_CorrectiveAction_AndReach_MaxAttempts()
  {
    final AtomicInteger i = new AtomicInteger();
    final OrElseFactory factory = Elsetility.await()
        .forever()
        .but().atMostThisManyTimes(3)
        .and().with().correctiveAction(() -> i.incrementAndGet());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
    assertThat(i.get(), is(equalTo(2)));
  }

  @Test
  void doNotCall_CorrectiveAction_IfOnlyOneAttempt()
  {
    final AtomicInteger i = new AtomicInteger();
    final OrElseFactory factory = Elsetility.await()
        .given().maxNumOfAttempts(1)
        .and().correctiveAction(() -> i.incrementAndGet());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
    assertThat(i.get(), is(equalTo(0)));
  }

  @Test
  void call_CorrectiveAction_AndExpireTimeout()
  {
    final AtomicInteger i = new AtomicInteger();
    final OrElseFactory factory = Elsetility.await()
        .given().unlimitedNumOfAttempts()
        .but().timeout(ofSeconds(1))
        .orElseDo(() -> i.incrementAndGet());
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> false));
    assertThat(i.get(), is(greaterThan(0)));
  }

  @Test
  void ignoreExceptionIn_Supplier()
  {
    final OrElseFactory factory = Elsetility.await()
        .given().maxNumOfAttempts(1)
        .and().ignoreExceptionsMatching(instanceOf(ArithmeticException.class));
    final Matcher<Integer> matcher = is(equalTo(0));
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> 1 / 0, matcher));
  }

  @Test
  void doNotIgnoreExceptionIn_Supplier()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    final Matcher<Integer> matcher = is(equalTo(0));
    assertThrows(ArithmeticException.class, () -> factory.until(() -> 1 / 0, matcher));
  }

  @Test
  void ignoreExceptionIn_CorrectiveAction()
  {
    final OrElseFactory factory = Elsetility.await()
        .given().maxNumOfAttempts(2)
        .and().ignoreExceptionsMatching(instanceOf(ArithmeticException.class))
        .with().correctiveAction(() ->
        {
          int i = 1 / 0;
        });
    final Matcher<Integer> matcher = is(equalTo(1));
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> 0, matcher));
  }

  @Test
  void doNotIgnoreExceptionIn_CorrectiveAction()
  {
    final OrElseFactory factory = Elsetility.await()
        .given().maxNumOfAttempts(2)
        .but().ifItFailsThenDo(() ->
        {
          int i = 1 / 0;
        });
    final Matcher<Integer> matcher = is(equalTo(1));
    assertThrows(ArithmeticException.class, () -> factory.until(() -> 0, matcher));
  }

  @Test
  void ignoreExceptionIn_Supplier_OrIn_CorrectiveAction_()
  {
    final Matcher<Throwable> ignoredExceptions = instanceOfAnyOf(
        ArithmeticException.class,
        NullPointerException.class
    );
    final OrElseFactory factory = Elsetility.await()
        .given().maxNumOfAttempts(2)
        .and().ignoreExceptionsMatching(ignoredExceptions)
        .with().correctiveAction(() ->
        {
          int i = 1 / 0;
        });
    final String s = null;
    final Matcher<Boolean> matcher = is(true);
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> s.isEmpty(), matcher));
  }

  @Test
  void ignoreExceptionIn_Condition()
  {
    final OrElseFactory factory = Elsetility.await()
        .given().maxNumOfAttempts(1)
        .and().ignoreExceptionsMatching(instanceOf(ArithmeticException.class));
    assertThrows(ConditionTimeoutException.class, () -> factory.until(() -> 1 / 0 == 0));
  }

  @Test
  void doNotIgnoreExceptionIn_Condition()
  {
    final OrElseFactory factory = Elsetility.await().given().maxNumOfAttempts(1);
    assertThrows(ArithmeticException.class, () -> factory.until(() -> 1 / 0 == 0));
  }
}
