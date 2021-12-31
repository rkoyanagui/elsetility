package com.rkoyanagui;

import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;

import com.rkoyanagui.core.OrElseFactory;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.awaitility.constraint.AtMostWaitConstraint;
import org.awaitility.constraint.WaitConstraint;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.ConditionFactory;
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
  protected static ThreadLocal<PollInterval> defaultPollInterval =
      ThreadLocal.withInitial(() -> DEFAULT_POLL_INTERVAL);

  /** The default wait constraint (initially, 10 seconds). */
  protected static ThreadLocal<WaitConstraint> defaultWaitConstraint =
      ThreadLocal.withInitial(() -> AtMostWaitConstraint.TEN_SECONDS);

  /**
   * The default poll delay (initially, {@code null}, meaning it will be the same duration as the
   * poll interval).
   */
  protected static ThreadLocal<Duration> defaultPollDelay =
      ThreadLocal.withInitial(() -> DEFAULT_POLL_DELAY);

  /** Default answer to the question 'Should catch all uncaught exceptions?' (initially, yes). */
  protected static volatile boolean defaultCatchUncaughtExceptions = true;

  /**
   * Default answer to the question 'Should ignore caught exceptions?' (initially, no, ignore
   * none).
   */
  protected static ThreadLocal<ExceptionIgnorer> defaultExceptionIgnorer =
      ThreadLocal.withInitial(() -> new PredicateExceptionIgnorer(e -> false));

  /** Default listener of condition evaluation results (initially, {@code null}). */
  protected static ThreadLocal<ConditionEvaluationListener<?>> defaultConditionEvaluationListener =
      ThreadLocal.withInitial(() -> null);

  /** Default condition evaluation executor service (initially, {@code null}). */
  protected static ThreadLocal<ExecutorLifecycle> defaultExecutorLifecycle =
      ThreadLocal.withInitial(() -> null);

  /** Default fail-fast condition (initially, {@code null}). */
  protected static ThreadLocal<FailFastCondition> defaultFailFastCondition =
      ThreadLocal.withInitial(() -> null);

  /** Default maximum number of attempts (initially, {@link Integer#MAX_VALUE}). */
  protected static ThreadLocal<Integer> defaultMaxAttempts =
      ThreadLocal.withInitial(() -> Integer.MAX_VALUE);

  /** Default corrective action (initially, "do nothing"). */
  protected static ThreadLocal<Runnable> defaultOrElseDo =
      ThreadLocal.withInitial(() -> () -> {});

  protected Elsetility()
  {
    super();
  }

  /**
   * Prevents the continuation of a program's execution for a fixed duration.
   *
   * @param duration duration to wait
   */
  public static void await(Duration duration)
  {
    Awaitility.await()
        .given()
        .pollDelay(duration)
        .forever()
        .until(() -> true);
  }

  /**
   * Start building an {@code await} statement.
   *
   * @return the condition factory
   */
  public static OrElseFactory await()
  {
    return await((String) null);
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
    final ConditionFactory conditionFactory = new ConditionFactory(alias,
        defaultWaitConstraint.get(), defaultPollInterval.get(), defaultPollDelay.get(),
        defaultCatchUncaughtExceptions, defaultExceptionIgnorer.get(),
        defaultConditionEvaluationListener.get(), defaultExecutorLifecycle.get(),
        defaultFailFastCondition.get());
    return new OrElseFactory(conditionFactory, defaultMaxAttempts.get(), defaultOrElseDo.get());
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
    defaultExceptionIgnorer.set(new PredicateExceptionIgnorer(e -> true));
  }

  /**
   * Instruct Awaitility to ignore caught exception of the given type during condition evaluation.
   * Exceptions will be treated as evaluating to <code>false</code>. Your test will not fail upon an
   * exception matching the supplied exception type, unless it times out.
   */
  public static void ignoreExceptionByDefault(final Class<? extends Throwable> exceptionType)
  {
    defaultExceptionIgnorer.set(
        new PredicateExceptionIgnorer(e -> e.getClass().equals(exceptionType))
    );
  }

  /**
   * Instruct Awaitility to ignore caught exceptions matching the given <code>predicate</code>
   * during condition evaluation. Exceptions will be treated as evaluating to <code>false</code>.
   * Your test will not fail upon an exception matching the supplied predicate, unless it times
   * out.
   */
  public static void ignoreExceptionsByDefaultMatching(final Predicate<? super Throwable> predicate)
  {
    defaultExceptionIgnorer.set(new PredicateExceptionIgnorer(predicate));
  }

  /**
   * Instruct Awaitility to ignore caught exceptions matching the supplied <code>matcher</code>
   * during condition evaluation. Exceptions will be treated as evaluating to <code>false</code>.
   * Your test will not fail upon an exception matching the supplied exception type, unless it times
   * out.
   */
  public static void ignoreExceptionsByDefaultMatching(final Matcher<? super Throwable> matcher)
  {
    defaultExceptionIgnorer.set(new HamcrestExceptionIgnorer(matcher));
  }

  /**
   * Creates a {@link org.hamcrest.Matcher} that matches when an object {@code o} is an instance of
   * (meaning, is a subclass of, or implements an interface of) any of the given classes.
   *
   * @param xs  expected superclasses or superinterfaces
   * @param <T> the type of the superclasses or superinterfaces
   * @return a {@link org.hamcrest.Matcher}
   */
  @SafeVarargs // Creating a stream from an array is safe
  public static <T> Matcher<T> instanceOfAnyOf(final Class<? extends T>... xs)
  {
    return anyOf(
        Stream.of(xs)
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
   * <pre>{@code
   * @Test(timeout = 2000L)
   * public void myTest() {
   *     Awaitility.pollInSameThread();
   *     await().forever().until(...);
   * }
   * }</pre>
   *
   * @since 3.0.0
   */
  public static void pollInSameThread()
  {
    defaultExecutorLifecycle.set(
        ExecutorLifecycle.withNormalCleanupBehavior(
            () -> InternalExecutorServiceFactory.sameThreadExecutorService()
        )
    );
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
    defaultExecutorLifecycle.set(ExecutorLifecycle.withoutCleanup(executorService));
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
    defaultExecutorLifecycle.set(
        ExecutorLifecycle.withNormalCleanupBehavior(
            () -> InternalExecutorServiceFactory.create(threadSupplier)
        )
    );
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
    defaultPollInterval.set(DEFAULT_POLL_INTERVAL);
    defaultPollDelay.set(DEFAULT_POLL_DELAY);
    defaultWaitConstraint.set(AtMostWaitConstraint.TEN_SECONDS);
    defaultCatchUncaughtExceptions = true;
    defaultConditionEvaluationListener.remove();
    defaultExecutorLifecycle.remove();
    defaultExceptionIgnorer.set(new PredicateExceptionIgnorer(e -> false));
    defaultFailFastCondition.remove();
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
    final ConditionFactory conditionFactory = new ConditionFactory(null,
        defaultWaitConstraint.get(), defaultPollInterval.get(), defaultPollDelay.get(),
        true, defaultExceptionIgnorer.get(),
        defaultConditionEvaluationListener.get(), defaultExecutorLifecycle.get(),
        defaultFailFastCondition.get());
    return new OrElseFactory(conditionFactory, defaultMaxAttempts.get(), defaultOrElseDo.get());
  }

  /**
   * Don't catch uncaught exceptions in other threads. This will <i>not</i> make the await statement
   * fail if exceptions occur in other threads.
   *
   * @return the condition factory
   */
  public static OrElseFactory dontCatchUncaughtExceptions()
  {
    final ConditionFactory conditionFactory = new ConditionFactory(null,
        defaultWaitConstraint.get(), defaultPollInterval.get(), defaultPollDelay.get(),
        false, defaultExceptionIgnorer.get(),
        defaultConditionEvaluationListener.get(), defaultExecutorLifecycle.get(),
        defaultFailFastCondition.get());
    return new OrElseFactory(conditionFactory, defaultMaxAttempts.get(), defaultOrElseDo.get());
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
    final ConditionFactory conditionFactory = new ConditionFactory(null,
        defaultWaitConstraint.get(), defaultPollInterval.get(), defaultPollDelay.get(),
        defaultCatchUncaughtExceptions, defaultExceptionIgnorer.get(),
        defaultConditionEvaluationListener.get(), defaultExecutorLifecycle.get(),
        defaultFailFastCondition.get());
    return new OrElseFactory(conditionFactory, defaultMaxAttempts.get(), defaultOrElseDo.get());
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
    return with();
  }

  /**
   * An alternative to using {@link #await()} if you want to specify a timeout directly.
   *
   * @param timeout the timeout
   * @return the condition factory
   */
  public static OrElseFactory waitAtMost(final Duration timeout)
  {
    final ConditionFactory conditionFactory = new ConditionFactory(null,
        defaultWaitConstraint.get().withMaxWaitTime(timeout), defaultPollInterval.get(),
        defaultPollDelay.get(), defaultCatchUncaughtExceptions, defaultExceptionIgnorer.get(),
        defaultConditionEvaluationListener.get(), defaultExecutorLifecycle.get(),
        defaultFailFastCondition.get());
    return new OrElseFactory(conditionFactory, defaultMaxAttempts.get(), defaultOrElseDo.get());
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
    final ConditionFactory conditionFactory = new ConditionFactory(null,
        defaultWaitConstraint.get().withMaxWaitTime(DurationFactory.of(value, unit)),
        defaultPollInterval.get(), defaultPollDelay.get(), defaultCatchUncaughtExceptions,
        defaultExceptionIgnorer.get(), defaultConditionEvaluationListener.get(),
        defaultExecutorLifecycle.get(), defaultFailFastCondition.get());
    return new OrElseFactory(conditionFactory, defaultMaxAttempts.get(), defaultOrElseDo.get());
  }

  /**
   * Sets the default poll interval that all await statements will use.
   *
   * @param pollInterval the poll interval
   * @param unit         the unit
   */
  public static void setDefaultPollInterval(final long pollInterval, final TimeUnit unit)
  {
    defaultPollInterval.set(new FixedPollInterval(DurationFactory.of(pollInterval, unit)));
  }

  /**
   * Sets the default poll delay all await statements will use.
   *
   * @param pollDelay the poll delay
   * @param unit      the unit
   */
  public static void setDefaultPollDelay(final long pollDelay, final TimeUnit unit)
  {
    defaultPollDelay.set(DurationFactory.of(pollDelay, unit));
  }

  /**
   * Sets the default timeout all await statements will use.
   *
   * @param timeout the timeout
   * @param unit    the unit
   */
  public static void setDefaultTimeout(final long timeout, final TimeUnit unit)
  {
    defaultWaitConstraint.set(
        defaultWaitConstraint.get().withMaxWaitTime(DurationFactory.of(timeout, unit))
    );
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
    defaultPollInterval.set(new FixedPollInterval(pollInterval));
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
    defaultPollInterval.set(pollInterval);
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
    defaultPollDelay.set(pollDelay);
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
    defaultWaitConstraint.set(
        defaultWaitConstraint.get().withMaxWaitTime(defaultTimeout)
    );
  }

  /**
   * Sets the default condition evaluation listener that all await statements will use.
   *
   * @param listener handles condition evaluation each time evaluation of a condition occurs. Works
   *                 only with Hamcrest matcher-based conditions.
   */
  public static void setDefaultConditionEvaluationListener(
      final ConditionEvaluationListener<?> listener)
  {
    defaultConditionEvaluationListener.set(listener);
  }

  /**
   * If the supplied Callable <i>ever</i> returns false, it indicates our condition will
   * <i>never</i> be true, and if so fail the system immediately. Throws a {@link
   * TerminalFailureException} if fail fast condition evaluates to <code>true</code>. If you want to
   * specify a more descriptive error message then use {@link #setDefaultFailFastCondition(String,
   * Callable)}.
   *
   * @param condition The terminal failure condition
   * @see #setDefaultFailFastCondition(String, Callable)
   */
  public static void setDefaultFailFastCondition(final Callable<Boolean> condition)
  {
    defaultFailFastCondition.set(new FailFastCondition(null, condition));
  }

  /**
   * If the supplied Callable <i>ever</i> returns false, it indicates our condition will
   * <i>never</i> be true, and if so fail the system immediately. Throws a {@link
   * TerminalFailureException} if fail fast condition evaluates to <code>true</code>.
   *
   * @param condition The terminal failure condition
   * @param reason    A descriptive reason why the fail fast condition has failed, will be included
   *                  in the {@link TerminalFailureException} thrown if <code>failFastCondition</code>
   *                  evaluates to
   *                  <code>true</code>.
   */
  public static void setDefaultFailFastCondition(
      final String reason,
      final Callable<Boolean> condition)
  {
    Elsetility.defaultFailFastCondition.set(new FailFastCondition(reason, condition));
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
