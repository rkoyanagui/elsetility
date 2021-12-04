package com.rkoyanagui;

import static java.time.Duration.ofMillis;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;

import com.rkoyanagui.core.OrElseFactory;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.awaitility.constraint.AtMostWaitConstraint;
import org.awaitility.constraint.WaitConstraint;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.DurationFactory;
import org.awaitility.core.ExceptionIgnorer;
import org.awaitility.core.ExecutorLifecycle;
import org.awaitility.core.FailFastCondition;
import org.awaitility.core.FieldSupplierBuilder;
import org.awaitility.core.HamcrestExceptionIgnorer;
import org.awaitility.core.InternalExecutorServiceFactory;
import org.awaitility.core.PredicateExceptionIgnorer;
import org.awaitility.core.TerminalFailureException;
import org.awaitility.pollinterval.FixedPollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.hamcrest.Matcher;

public class Elsetility
{
  protected static final Duration DEFAULT_POLL_DELAY = null;

  protected static final PollInterval DEFAULT_POLL_INTERVAL =
      new FixedPollInterval(ONE_HUNDRED_MILLISECONDS);

  /** The default poll interval (initially, 100 ms). */
  protected static volatile PollInterval defaultPollInterval = DEFAULT_POLL_INTERVAL;

  /** The default wait constraint (initially, 10 seconds). */
  protected static volatile WaitConstraint defaultWaitConstraint = AtMostWaitConstraint.TEN_SECONDS;

  /**
   * The default poll delay (initially, {@code null}, meaning it will be the same duration as the
   * poll interval).
   */
  protected static volatile Duration defaultPollDelay = DEFAULT_POLL_DELAY;

  /** Default answer to the question 'Should catch all uncaught exceptions?' (initially, yes). */
  protected static volatile boolean defaultCatchUncaughtExceptions = true;

  /**
   * Default answer to the question 'Should ignore caught exceptions?' (initially, no, ignore
   * none).
   */
  protected static volatile ExceptionIgnorer defaultExceptionIgnorer = new PredicateExceptionIgnorer(
      e -> false);

  /** Default listener of condition evaluation results. */
  protected static volatile ConditionEvaluationListener defaultConditionEvaluationListener = null;

  /** Default condition evaluation executor service. */
  protected static volatile ExecutorLifecycle defaultExecutorLifecycle = null;

  /** Default fail-fast condition (initially, {@code null}). */
  protected static volatile FailFastCondition defaultFailFastCondition = null;

  /** Default maximum number of attempts (initially, {@link Integer#MAX_VALUE}). */
  protected static volatile Integer defaultMaxAttempts = Integer.MAX_VALUE;

  /** Default corrective action. */
  protected static volatile Runnable defaultOrElseDo = () -> {};

  protected Elsetility()
  {
    super();
  }

  /**
   * Holds a program's execution for a fixed number of seconds.
   *
   * @param seconds number of seconds to wait
   */
  public static void await(final long seconds)
  {
    final long pollDelay = 0L;
    final long pollInterval = 1_000L;
    final LongAdder counter = new LongAdder();
    final ScheduledFuture<?> scheduledFuture = Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(
            () -> counter.increment(),
            pollDelay,
            pollInterval,
            TimeUnit.MILLISECONDS);

    Awaitility.await()
        .pollDelay(ofMillis(pollDelay))
        .pollInterval(ofMillis(pollInterval))
        .forever()
        .untilAdder(counter, greaterThan(seconds));

    scheduledFuture.cancel(true);
  }

  /**
   * Start building an {@code await} statement.
   *
   * @return the condition factory
   */
  public static OrElseFactory await()
  {
    return await(null);
  }

  /**
   * Start building a named {@code await} statement. This is useful in cases when you have several
   * {@code await}s in your test and you need to tell them apart. If a named {@code await} times
   * out, the {@code alias} will be displayed, indicating which {@code await} statement failed.
   *
   * @param alias the alias that will be shown if the await times out.
   * @return the condition factory
   */
  public static OrElseFactory await(final String alias)
  {
    return new OrElseFactory(alias, defaultWaitConstraint, defaultPollInterval, defaultPollDelay,
        defaultCatchUncaughtExceptions, defaultExceptionIgnorer, defaultConditionEvaluationListener,
        defaultExecutorLifecycle, defaultFailFastCondition, defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * Instruct Awaitility to catch uncaught exceptions from other threads by default. This is useful
   * in multi-threaded systems when you want your test to fail regardless of which thread throwing
   * the exception. Default is
   * <code>true</code>.
   */
  public static void catchUncaughtExceptionsByDefault()
  {
    defaultCatchUncaughtExceptions = true;
  }

  /**
   * Instruct Awaitility not to catch uncaught exceptions from other threads. Your test will not
   * fail if another thread throws an exception.
   */
  public static void doNotCatchUncaughtExceptionsByDefault()
  {
    defaultCatchUncaughtExceptions = false;
  }

  /**
   * Instruct Awaitility to ignore caught or uncaught exceptions during condition evaluation.
   * Exceptions will be treated as evaluating to <code>false</code>. Your test will not fail upon an
   * exception, unless it times out.
   */
  public static void ignoreExceptionsByDefault()
  {
    defaultExceptionIgnorer = new PredicateExceptionIgnorer(e -> true);
  }

  /**
   * Instruct Awaitility to ignore caught exception of the given type during condition evaluation.
   * Exceptions will be treated as evaluating to <code>false</code>. Your test will not fail upon an
   * exception matching the supplied exception type, unless it times out.
   */
  public static void ignoreExceptionByDefault(final Class<? extends Throwable> exceptionType)
  {
    defaultExceptionIgnorer = new PredicateExceptionIgnorer(
        e -> e.getClass().equals(exceptionType));
  }

  /**
   * Instruct Awaitility to ignore caught exceptions matching the given <code>predicate</code>
   * during condition evaluation. Exceptions will be treated as evaluating to <code>false</code>.
   * Your test will not fail upon an exception matching the supplied predicate, unless it times
   * out.
   */
  public static void ignoreExceptionsByDefaultMatching(final Predicate<? super Throwable> predicate)
  {
    defaultExceptionIgnorer = new PredicateExceptionIgnorer(predicate);
  }

  /**
   * Instruct Awaitility to ignore caught exceptions matching the supplied <code>matcher</code>
   * during condition evaluation. Exceptions will be treated as evaluating to <code>false</code>.
   * Your test will not fail upon an exception matching the supplied exception type, unless it times
   * out.
   */
  public static void ignoreExceptionsByDefaultMatching(final Matcher<? super Throwable> matcher)
  {
    defaultExceptionIgnorer = new HamcrestExceptionIgnorer(matcher);
  }

  // TODO javadoc
  public static <T> Matcher<? super T> instanceOfAnyOf(
      final Collection<Class<? extends T>> xs)
  {
    return anyOf(
        xs.stream()
            .map(x -> instanceOf(x))
            .collect(Collectors.toList())
    );
  }

  /**
   * Instructs Awaitility to execute the polling of the condition from the same as the test. This is
   * an advanced feature and you should be careful when combining this with conditions that wait
   * forever (or a long time) since Awaitility cannot interrupt the thread when using the same
   * thread as the test. For safety you should always combine tests using this feature with a test
   * framework specific timeout, for example in JUnit:
   * <pre>
   * @Test(timeout = 2000L)
   * public void myTest() {
   *     Awaitility.pollInSameThread();
   *     await().forever().until(...);
   * }
   * </pre>
   *
   * @since 3.0.0
   */
  public static void pollInSameThread()
  {
    defaultExecutorLifecycle = ExecutorLifecycle.withNormalCleanupBehavior(
        InternalExecutorServiceFactory::sameThreadExecutorService);
  }

  /**
   * Specify the executor service whose threads will be used to evaluate the poll condition in
   * Awaitility. Note that the executor service must be shutdown manually! This is an advanced
   * feature and it should only be used sparingly.
   *
   * @param executorService The executor service that Awaitility will use when polling condition
   *                        evaluations
   * @since 3.0.0
   */
  public static void pollExecutorService(final ExecutorService executorService)
  {
    defaultExecutorLifecycle = ExecutorLifecycle.withoutCleanup(executorService);
  }

  /**
   * Specify a thread supplier whose thread will be used to evaluate the poll condition in
   * Awaitility. The supplier will be called only once and the thread it returns will be reused
   * during all condition evaluations. This is an advanced feature and it should only be used
   * sparingly.
   *
   * @param threadSupplier A supplier of the thread that Awaitility will use when polling
   * @since 3.0.0
   */
  public static void pollThread(final Function<Runnable, Thread> threadSupplier)
  {
    defaultExecutorLifecycle = ExecutorLifecycle.withNormalCleanupBehavior(
        () -> InternalExecutorServiceFactory.create(threadSupplier));
  }

  /**
   * Reset the timeout, poll interval, poll delay, uncaught exception handling to their default
   * values:
   * <p>&nbsp;</p>
   * <ul>
   * <li>timeout - 10 seconds</li>
   * <li>poll interval - 100 milliseconds</li>
   * <li>poll delay - 100 milliseconds</li>
   * <li>Catch all uncaught exceptions - true</li>
   * <li>Do not ignore caught exceptions</li>
   * <li>Don't handle condition evaluation results</li>
   * <li>No fail fast condition</li>
   * </ul>
   */
  public static void reset()
  {
    defaultPollInterval = DEFAULT_POLL_INTERVAL;
    defaultPollDelay = DEFAULT_POLL_DELAY;
    defaultWaitConstraint = AtMostWaitConstraint.TEN_SECONDS;
    defaultCatchUncaughtExceptions = true;
    defaultConditionEvaluationListener = null;
    defaultExecutorLifecycle = null;
    defaultExceptionIgnorer = new PredicateExceptionIgnorer(e -> false);
    defaultFailFastCondition = null;
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  /**
   * Catching uncaught exceptions in other threads. This will make the await statement fail even if
   * exceptions occur in other threads. This is the default behavior.
   *
   * @return the condition factory
   */
  public static OrElseFactory catchUncaughtExceptions()
  {
    return new OrElseFactory(null, defaultWaitConstraint, defaultPollInterval, defaultPollDelay,
        defaultCatchUncaughtExceptions, defaultExceptionIgnorer, defaultConditionEvaluationListener,
        defaultExecutorLifecycle, defaultFailFastCondition, defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * Don't catch uncaught exceptions in other threads. This will <i>not</i> make the await statement
   * fail if exceptions occur in other threads.
   *
   * @return the condition factory
   */
  public static OrElseFactory dontCatchUncaughtExceptions()
  {
    return new OrElseFactory(null, defaultWaitConstraint, defaultPollInterval, defaultPollDelay,
        false, defaultExceptionIgnorer, defaultConditionEvaluationListener,
        defaultExecutorLifecycle, defaultFailFastCondition, defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * Start constructing an await statement with some settings. E.g.
   * <p>
   * <pre>
   * with().pollInterval(20, MILLISECONDS).await().until(somethingHappens());
   * </pre>
   *
   * @return the condition factory
   */
  public static OrElseFactory with()
  {
    return new OrElseFactory(null, defaultWaitConstraint, defaultPollInterval, defaultPollDelay,
        defaultCatchUncaughtExceptions, defaultExceptionIgnorer, defaultConditionEvaluationListener,
        defaultExecutorLifecycle, defaultFailFastCondition, defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * Start constructing an await statement given some settings. E.g.
   * <p>
   * <pre>
   * given().pollInterval(20, MILLISECONDS).then().await().until(somethingHappens());
   * </pre>
   *
   * @return the condition factory
   */
  public static OrElseFactory given()
  {
    return new OrElseFactory(null, defaultWaitConstraint, defaultPollInterval, defaultPollDelay,
        defaultCatchUncaughtExceptions, defaultExceptionIgnorer, defaultConditionEvaluationListener,
        defaultExecutorLifecycle, defaultFailFastCondition, defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * An alternative to using {@link #await()} if you want to specify a timeout directly.
   *
   * @param timeout the timeout
   * @return the condition factory
   */
  public static OrElseFactory waitAtMost(final Duration timeout)
  {
    return new OrElseFactory(null, defaultWaitConstraint.withMaxWaitTime(timeout),
        defaultPollInterval, defaultPollDelay, defaultCatchUncaughtExceptions,
        defaultExceptionIgnorer, defaultConditionEvaluationListener, defaultExecutorLifecycle,
        defaultFailFastCondition, defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * An alternative to using {@link #await()} if you want to specify a timeout directly.
   *
   * @param value the value
   * @param unit  the unit
   * @return the condition factory
   */
  public static OrElseFactory waitAtMost(final long value, final TimeUnit unit)
  {
    return new OrElseFactory(null,
        defaultWaitConstraint.withMaxWaitTime(DurationFactory.of(value, unit)), defaultPollInterval,
        defaultPollDelay, defaultCatchUncaughtExceptions, defaultExceptionIgnorer,
        defaultConditionEvaluationListener, defaultExecutorLifecycle, defaultFailFastCondition,
        defaultMaxAttempts, defaultOrElseDo);
  }

  /**
   * Sets the default poll interval that all await statements will use.
   *
   * @param pollInterval the poll interval
   * @param unit         the unit
   */
  public static void setDefaultPollInterval(final long pollInterval, final TimeUnit unit)
  {
    defaultPollInterval = new FixedPollInterval(DurationFactory.of(pollInterval, unit));
  }

  /**
   * Sets the default poll delay all await statements will use.
   *
   * @param pollDelay the poll delay
   * @param unit      the unit
   */
  public static void setDefaultPollDelay(final long pollDelay, final TimeUnit unit)
  {
    defaultPollDelay = DurationFactory.of(pollDelay, unit);
  }

  /**
   * Sets the default timeout all await statements will use.
   *
   * @param timeout the timeout
   * @param unit    the unit
   */
  public static void setDefaultTimeout(final long timeout, final TimeUnit unit)
  {
    final WaitConstraint waitConstraint =
        defaultWaitConstraint.withMaxWaitTime(DurationFactory.of(timeout, unit));
    defaultWaitConstraint = waitConstraint;
  }

  /**
   * Sets the default poll interval that all await statements will use.
   *
   * @param pollInterval the new default poll interval
   */
  public static void setDefaultPollInterval(final Duration pollInterval)
  {
    if (pollInterval == null)
    {
      throw new IllegalArgumentException("You must specify a poll interval (was null).");
    }
    defaultPollInterval = new FixedPollInterval(pollInterval);
  }

  /**
   * Sets the default poll interval that all await statements will use.
   *
   * @param pollInterval the new default poll interval
   */
  public static void setDefaultPollInterval(final PollInterval pollInterval)
  {
    if (pollInterval == null)
    {
      throw new IllegalArgumentException("You must specify a poll interval (was null).");
    }
    defaultPollInterval = pollInterval;
  }

  /**
   * Sets the default poll delay that all await statements will use.
   *
   * @param pollDelay the new default poll delay
   */
  public static void setDefaultPollDelay(final Duration pollDelay)
  {
    if (pollDelay == null)
    {
      throw new IllegalArgumentException("You must specify a poll delay (was null).");
    }
    defaultPollDelay = pollDelay;
  }

  /**
   * Sets the default timeout that all await statements will use.
   *
   * @param defaultTimeout the new default timeout
   */
  public static void setDefaultTimeout(final Duration defaultTimeout)
  {
    if (defaultTimeout == null)
    {
      throw new IllegalArgumentException("You must specify a default timeout (was null).");
    }
    final WaitConstraint waitConstraint = defaultWaitConstraint.withMaxWaitTime(defaultTimeout);
    defaultWaitConstraint = waitConstraint;
  }

  /**
   * Sets the default condition evaluation listener that all await statements will use.
   *
   * @param defaultConditionEvaluationListener handles condition evaluation each time evaluation of
   *                                           a condition occurs. Works only with Hamcrest
   *                                           matcher-based conditions.
   */
  public static void setDefaultConditionEvaluationListener(
      final ConditionEvaluationListener defaultConditionEvaluationListener)
  {
    Elsetility.defaultConditionEvaluationListener = defaultConditionEvaluationListener;
  }

  /**
   * If the supplied Callable <i>ever</i> returns false, it indicates our condition will
   * <i>never</i> be true, and if so fail the system immediately. Throws a {@link
   * TerminalFailureException} if fail fast condition evaluates to <code>true</code>. If you want to
   * specify a more descriptive error message then use {@link #setDefaultFailFastCondition(String,
   * Callable)}.
   *
   * @param defaultFailFastCondition The terminal failure condition
   * @see #setDefaultFailFastCondition(String, Callable)
   */
  public static void setDefaultFailFastCondition(final Callable<Boolean> defaultFailFastCondition)
  {
    Elsetility.defaultFailFastCondition =
        new FailFastCondition(null, defaultFailFastCondition);
  }

  /**
   * If the supplied Callable <i>ever</i> returns false, it indicates our condition will
   * <i>never</i> be true, and if so fail the system immediately. Throws a {@link
   * TerminalFailureException} if fail fast condition evaluates to <code>true</code>.
   *
   * @param defaultFailFastCondition The terminal failure condition
   * @param failFastFailureReason    A descriptive reason why the fail fast condition has failed,
   *                                 will be included in the {@link TerminalFailureException} thrown
   *                                 if <code>failFastCondition</code> evaluates to
   *                                 <code>true</code>.
   */
  public static void setDefaultFailFastCondition(
      final String failFastFailureReason,
      final Callable<Boolean> defaultFailFastCondition)
  {
    Elsetility.defaultFailFastCondition =
        new FailFastCondition(failFastFailureReason, defaultFailFastCondition);
  }

  /**
   * Await until an instance field matches something. E.g.
   * <p>
   * <pre>
   * await().until(fieldIn(service).ofType(int.class).andWithName("fieldName"), greaterThan(2));
   * </pre>
   * <p>
   * Here Awaitility waits until a field with name <code>fieldName</code> and of the
   * <code>int.class</code> in object <code>service</code> is greater than 2.
   * <p>
   * Note that the field must be thread-safe in order to guarantee correct behavior.
   *
   * @param object The object that contains the field.
   * @return A field supplier builder which lets you specify the parameters needed to find the
   * field.
   */
  public static FieldSupplierBuilder fieldIn(final Object object)
  {
    return new FieldSupplierBuilder(object);
  }

  /**
   * Await until a static field matches something. E.g.
   * <p>
   * <pre>
   * await().until(fieldIn(Service.class).ofType(int.class).andWithName("fieldName"), greaterThan(2));
   * </pre>
   * <p>
   * Here Awaitility waits until a static field with name <code>fieldName</code> and of the
   * <code>int.class</code> in object <code>service</code> is greater than 2.
   * <p>
   * Note that the field must be thread-safe in order to guarantee correct behavior.
   *
   * @param clazz The class that contains the static field.
   * @return A field supplier builder which lets you specify the parameters needed to find the
   * field.
   */
  public static FieldSupplierBuilder fieldIn(final Class<?> clazz)
  {
    return new FieldSupplierBuilder(clazz);
  }
}
