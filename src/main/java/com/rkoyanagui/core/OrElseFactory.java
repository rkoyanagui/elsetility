package com.rkoyanagui.core;

import com.rkoyanagui.wrappers.AssertionWrapper;
import com.rkoyanagui.wrappers.CallableWrapper;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import org.awaitility.constraint.WaitConstraint;
import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ConditionTimeoutException;
import org.awaitility.core.DurationFactory;
import org.awaitility.core.ExceptionIgnorer;
import org.awaitility.core.ExecutorLifecycle;
import org.awaitility.core.FailFastCondition;
import org.awaitility.core.TerminalFailureException;
import org.awaitility.core.ThrowingRunnable;
import org.awaitility.pollinterval.FixedPollInterval;
import org.awaitility.pollinterval.PollInterval;
import org.hamcrest.Matcher;

/**
 * A builder for creating a conditional wait, using
 * <a href="https://github.com/awaitility/awaitility">Awaitility</a>. Adds some extra
 * functionality.
 */
@SuppressWarnings("unused")
public class OrElseFactory
{
  protected final ConditionFactory conditionFactory;

  /** The maximum number of attempts. */
  protected final Integer maxAttempts;

  /** The corrective action to take when a condition fails. */
  protected final Runnable correctiveAction;

  /**
   * Instantiates a new condition factory.
   *
   * @param alias                       the alias
   * @param timeoutConstraint           the timeout constraint
   * @param pollInterval                the poll interval
   * @param pollDelay                   the poll delay
   * @param catchUncaughtExceptions     whether to catch uncaught exceptions
   * @param exceptionIgnorer            a mechanism to ignore exceptions
   * @param conditionEvaluationListener the condition evaluation listener
   * @param executorLifecycle           the executor lifecycle
   * @param failFastCondition           the fail-fast condition
   * @param maxAttempts                 the maximum number of attempts
   * @param correctiveAction            the corrective action to take when a condition fails
   */
  public OrElseFactory(
      final String alias,
      final WaitConstraint timeoutConstraint,
      final PollInterval pollInterval,
      final Duration pollDelay,
      final boolean catchUncaughtExceptions,
      final ExceptionIgnorer exceptionIgnorer,
      final ConditionEvaluationListener conditionEvaluationListener,
      final ExecutorLifecycle executorLifecycle,
      final FailFastCondition failFastCondition,
      final Integer maxAttempts,
      final Runnable correctiveAction)
  {
    verifyParams(maxAttempts, correctiveAction);
    this.conditionFactory = new ConditionFactory(alias, timeoutConstraint, pollInterval, pollDelay,
        catchUncaughtExceptions, exceptionIgnorer, conditionEvaluationListener, executorLifecycle,
        failFastCondition);
    this.maxAttempts = maxAttempts;
    this.correctiveAction = correctiveAction;
  }

  public OrElseFactory(
      final ConditionFactory conditionFactory,
      final Integer maxAttempts,
      final Runnable correctiveAction)
  {
    verifyParams(maxAttempts, correctiveAction);
    this.conditionFactory = conditionFactory;
    this.maxAttempts = maxAttempts;
    this.correctiveAction = correctiveAction;
  }

  protected static void verifyParams(
      final Integer maxAttempts,
      final Runnable correctiveAction)
  {
    final String msg = "'%s' cannot be null.";
    verifyNonNull(maxAttempts, String.format(msg, "maxAttempts"));
    if (maxAttempts < 1)
    {
      throw new IllegalArgumentException("'maxAttempts' cannot be less than 1 (one).");
    }
    verifyNonNull(correctiveAction, String.format(msg, "correctiveAction"));
  }

  protected static <T> T verifyNonNull(final T obj, final String msg)
  {
    if (obj == null)
    {
      throw new IllegalArgumentException(msg);
    }
    return obj;
  }

  /**
   * Sets a maximum number of attempts to evaluate the condition. If both {@code maxAttempts} and
   * {@code timeout} have been set, then the wait ends whenever any of these two limits is reached.
   *
   * @param maxAttempts maximum number of attempts
   * @return the condition factory
   */
  public OrElseFactory maxNumOfAttempts(final Integer maxAttempts)
  {
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Syntactic sugar.
   *
   * @see OrElseFactory#maxNumOfAttempts(Integer)
   */
  public OrElseFactory atMostThisManyTimes(final Integer maxAttempts)
  {
    return maxNumOfAttempts(maxAttempts);
  }

  /**
   * Sets the maximum number of attempts as unlimited. So the only limiting factor is defined by
   * {@code timeout}. <p/> If both {@code maxAttempts} and {@code timeout} have been set as
   * <i>unlimited</i>, then the wait will never terminate unless the condition evaluates to
   * {@link Boolean#TRUE}, which is not advised, and could leave program execution hanging forever.
   *
   * @return the condition factory
   */
  public OrElseFactory unlimitedNumOfAttempts()
  {
    return maxNumOfAttempts(Integer.MAX_VALUE);
  }

  /**
   * Syntactic sugar.
   *
   * @see OrElseFactory#unlimitedNumOfAttempts()
   */
  public OrElseFactory asManyTimesAsItTakes()
  {
    return unlimitedNumOfAttempts();
  }

  /**
   * Sets what corrective action should be taken every time the condition fails. The default value
   * is {@code () -> {}}, that is, "do nothing".
   * <p>
   * It only makes sense to perform a corrective action if a new attempt is to be made. For this
   * reason, if the condition fails and it happens to be the last iteration (meaning either {@code
   * maxAttempts} or {@code timeout} has been reached), then, after this last attempt, the {@code
   * correctiveAction} is not taken.
   * <p>
   * Also, if {@code maxAttempts} is set to {@code 1}, that is, the condition should be tested only
   * once, then the {@code correctiveAction} is not taken.
   * <p>
   * Example using Java 7 Runnable:
   * <pre>
   * {@code
   *   await().correctiveAction(new Runnable() {
   *     public void run() {
   *       addBacon();
   *     }
   *   })
   *   .until(() -> eatSalad(), is(delicious()));
   * }
   * </pre>
   * <p>
   * Example using Java 8 lambda expressions:
   * <pre>
   * {@code
   * await()
   *   .correctiveAction(() -> addBacon())
   *   .until(() -> eatSalad(), is(delicious()));
   * }
   * </pre>
   *
   * @param correctiveAction an action to be taken every time the condition fails
   * @return the condition factory
   */
  public OrElseFactory correctiveAction(final Runnable correctiveAction)
  {
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Syntactic sugar.
   *
   * @see OrElseFactory#correctiveAction(Runnable)
   */
  public OrElseFactory orElseDo(final Runnable correctiveAction)
  {
    return correctiveAction(correctiveAction);
  }

  /**
   * Syntactic sugar.
   *
   * @see OrElseFactory#correctiveAction(Runnable)
   */
  public OrElseFactory ifItFailsThenDo(final Runnable correctiveAction)
  {
    return correctiveAction(correctiveAction);
  }

  /**
   * Sets {@code correctiveAction} to "do nothing".
   *
   * @see OrElseFactory#correctiveAction(Runnable)
   */
  public OrElseFactory noCorrectiveAction()
  {
    return correctiveAction(() -> {});
  }

  /**
   * Syntactic sugar.
   *
   * @see OrElseFactory#noCorrectiveAction()
   */
  public OrElseFactory orElseDoNothing()
  {
    return correctiveAction(() -> {});
  }

  /**
   * Syntactic sugar.
   *
   * @see OrElseFactory#noCorrectiveAction()
   */
  public OrElseFactory ifItFailsThenDoNothing()
  {
    return correctiveAction(() -> {});
  }

  /**
   * Handle condition evaluation results each time evaluation of a condition occurs. Works only with
   * a Hamcrest matcher-based condition.
   *
   * @param conditionEvaluationListener the condition evaluation listener
   * @return the condition factory
   */
  public OrElseFactory conditionEvaluationListener(
      final ConditionEvaluationListener conditionEvaluationListener)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.conditionEvaluationListener(conditionEvaluationListener);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await at most <code>timeout</code> before throwing a timeout exception.
   *
   * @param timeout the timeout
   * @return the condition factory
   */
  public OrElseFactory timeout(final Duration timeout)
  {
    return atMost(timeout);
  }

  /**
   * Await at most <code>timeout</code> before throwing a timeout exception.
   *
   * @param timeout the timeout
   * @return the condition factory
   */
  public OrElseFactory atMost(final Duration timeout)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.atMost(timeout);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await at the predicate holds during at least <code>timeout</code>
   *
   * @param timeout the timeout
   * @return the condition factory
   */
  public OrElseFactory during(final Duration timeout)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.during(timeout);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await at the predicate holds during at least <code>timeout</code>
   *
   * @param timeout the timeout
   * @param unit    the unit
   * @return the condition factory
   */
  public OrElseFactory during(final long timeout, final TimeUnit unit)
  {
    return during(DurationFactory.of(timeout, unit));
  }

  /**
   * Set the alias
   *
   * @param alias alias
   * @return the condition factory
   * @see org.awaitility.Awaitility#await(String)
   */
  public OrElseFactory alias(final String alias)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.alias(alias);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Condition has to be evaluated not earlier than <code>timeout</code> before throwing a timeout
   * exception.
   *
   * @param timeout the timeout
   * @return the condition factory
   */
  public OrElseFactory atLeast(final Duration timeout)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.atLeast(timeout);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Condition has to be evaluated not earlier than <code>timeout</code> before throwing a timeout
   * exception.
   *
   * @param timeout the timeout
   * @param unit    the unit
   * @return the condition factory
   */
  public OrElseFactory atLeast(final long timeout, final TimeUnit unit)
  {
    return atLeast(DurationFactory.of(timeout, unit));
  }

  /**
   * Specifies the duration window which has to be satisfied during operation execution. In case
   * operation is executed before <code>atLeast</code> or after <code>atMost</code> timeout
   * exception is thrown.
   *
   * @param atLeast lower part of execution window
   * @param atMost  upper part of execution window
   * @return the condition factory
   */
  public OrElseFactory between(final Duration atLeast, final Duration atMost)
  {
    return atLeast(atLeast).and().atMost(atMost);
  }

  /**
   * Specifies the duration window which has to be satisfied during operation execution. In case
   * operation is executed before <code>atLeastDuration</code> or after <code>atMostDuration</code>
   * timeout exception is thrown.
   *
   * @param atLeastDuration lower part of execution window
   * @param atMostDuration  upper part of execution window
   * @return the condition factory
   */
  public OrElseFactory between(
      final long atLeastDuration,
      final TimeUnit atLeastTimeUnit,
      final long atMostDuration,
      final TimeUnit atMostTimeUnit)
  {
    return between(DurationFactory.of(atLeastDuration, atLeastTimeUnit),
        DurationFactory.of(atMostDuration, atMostTimeUnit));
  }

  /**
   * Await forever until the condition is satisfied. Caution: You can block subsequent tests and the
   * entire build can hang indefinitely, it's recommended to always use a timeout.
   *
   * @return the condition factory
   */
  public OrElseFactory forever()
  {
    final ConditionFactory conditionFactory = this.conditionFactory.forever();
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Specify the polling interval Awaitility will use for this await statement. This means the
   * frequency in which the condition is checked for completion.
   * <p>
   * Note that the poll delay will be automatically set as to the same value as the interval (if
   * using a {@link FixedPollInterval}) unless it's specified explicitly using {@link
   * #pollDelay(Duration)}, {@link #pollDelay(long, TimeUnit)} or {@link
   * org.awaitility.core.ConditionFactory#pollDelay(java.time.Duration)}.
   * </p>
   *
   * @param pollInterval the poll interval
   * @return the condition factory
   */
  public OrElseFactory pollInterval(final Duration pollInterval)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.pollInterval(pollInterval);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await at most <code>timeout</code> before throwing a timeout exception.
   *
   * @param timeout the timeout
   * @param unit    the unit
   * @return the condition factory
   */
  public OrElseFactory timeout(final long timeout, final TimeUnit unit)
  {
    return atMost(timeout, unit);
  }

  /**
   * Specify the delay that will be used before Awaitility starts polling for the result the first
   * time. If you don't specify a poll delay explicitly it'll be the same as the poll interval.
   *
   * @param delay the delay
   * @param unit  the unit
   * @return the condition factory
   */
  public OrElseFactory pollDelay(final long delay, final TimeUnit unit)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.pollDelay(delay, unit);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Specify the delay that will be used before Awaitility starts polling for the result the first
   * time. If you don't specify a poll delay explicitly it'll be the same as the poll interval.
   *
   * @param pollDelay the poll delay
   * @return the condition factory
   */
  public OrElseFactory pollDelay(final Duration pollDelay)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.pollDelay(pollDelay);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await at most <code>timeout</code> before throwing a timeout exception.
   *
   * @param timeout the timeout
   * @param unit    the unit
   * @return the condition factory
   */
  public OrElseFactory atMost(final long timeout, final TimeUnit unit)
  {
    return atMost(DurationFactory.of(timeout, unit));
  }

  /**
   * Specify the polling interval Awaitility will use for this await statement. This means the
   * frequency in which the condition is checked for completion.
   * <p>&nbsp;</p>
   * Note that the poll delay will be automatically set as to the same value as the interval unless
   * it's specified explicitly using {@link #pollDelay(Duration)}, {@link #pollDelay(long,
   * TimeUnit)} or {@link org.awaitility.core.ConditionFactory#pollDelay(java.time.Duration)} , or
   * ConditionFactory#andWithPollDelay(long, TimeUnit)}. This is the same as creating a {@link
   * FixedPollInterval}.
   *
   * @param pollInterval the poll interval
   * @param unit         the unit
   * @return the condition factory
   * @see FixedPollInterval
   */
  public OrElseFactory pollInterval(final long pollInterval, final TimeUnit unit)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.pollInterval(pollInterval, unit);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  public OrElseFactory pollInterval(final PollInterval pollInterval)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.pollInterval(pollInterval);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Instruct Awaitility to catch uncaught exceptions from other threads. This is useful in
   * multi-threaded systems when you want your test to fail regardless of which thread throwing the
   * exception. Default is
   * <code>true</code>.
   *
   * @return the condition factory
   */
  public OrElseFactory catchUncaughtExceptions()
  {
    final ConditionFactory conditionFactory = this.conditionFactory.catchUncaughtExceptions();
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Instruct Awaitility to ignore exceptions instance of the supplied exceptionType type.
   * Exceptions will be treated as evaluating to <code>false</code>. This is useful in situations
   * where the evaluated conditions may temporarily throw exceptions.
   * <p/>
   * <p>If you want to ignore a specific exceptionType then use {@link #ignoreException(Class)}</p>
   *
   * @param exceptionType The exception type (hierarchy) to ignore
   * @return the condition factory
   */
  public OrElseFactory ignoreExceptionsInstanceOf(final Class<? extends Throwable> exceptionType)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.ignoreExceptionsInstanceOf(exceptionType);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Instruct Awaitility to ignore a specific exception and <i>no</i> subclasses of this exception.
   * Exceptions will be treated as evaluating to <code>false</code>. This is useful in situations
   * where the evaluated conditions may temporarily throw exceptions.
   * <p>If you want to ignore a subtypes of this exception then use {@link
   * #ignoreExceptionsInstanceOf(Class)}} </p>
   *
   * @param exceptionType The exception type to ignore
   * @return the condition factory
   */
  public OrElseFactory ignoreException(final Class<? extends Throwable> exceptionType)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.ignoreException(exceptionType);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Instruct Awaitility to ignore <i>all</i> exceptions that occur during evaluation. Exceptions
   * will be treated as evaluating to
   * <code>false</code>. This is useful in situations where the evaluated
   * conditions may temporarily throw exceptions.
   *
   * @return the condition factory.
   */
  public OrElseFactory ignoreExceptions()
  {
    return ignoreExceptionsMatching(e -> true);
  }

  /**
   * Instruct Awaitility to not ignore any exceptions that occur during evaluation. This is only
   * useful if Awaitility is configured to ignore exceptions by default but you want to have a
   * different behavior for a single test case.
   *
   * @return the condition factory.
   */
  public OrElseFactory ignoreNoExceptions()
  {
    return ignoreExceptionsMatching(e -> false);
  }

  /**
   * Instruct Awaitility to ignore exceptions that occur during evaluation and matches the supplied
   * Hamcrest matcher. Exceptions will be treated as evaluating to
   * <code>false</code>. This is useful in situations where the evaluated conditions may
   * temporarily throw exceptions.
   *
   * @return the condition factory.
   */
  public OrElseFactory ignoreExceptionsMatching(final Matcher<? super Throwable> matcher)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.ignoreExceptionsMatching(matcher);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Instruct Awaitility to ignore exceptions that occur during evaluation and matches the supplied
   * <code>predicate</code>. Exceptions will be treated as evaluating to
   * <code>false</code>. This is useful in situations where the evaluated conditions may
   * temporarily throw exceptions.
   *
   * @return the condition factory.
   */
  public OrElseFactory ignoreExceptionsMatching(final Predicate<? super Throwable> predicate)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.ignoreExceptionsMatching(predicate);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await for an asynchronous operation. This method returns the same {@link
   * org.awaitility.core.ConditionFactory} instance and is used only to get a more fluent-like
   * syntax.
   *
   * @return the condition factory
   */
  public OrElseFactory await()
  {
    return this;
  }

  /**
   * Await for an asynchronous operation and give this await instance a particular name. This is
   * useful in cases when you have several await statements in one test and you want to know which
   * one that fails (the alias will be shown if a timeout exception occurs).
   *
   * @param alias the alias
   * @return the condition factory
   */
  public OrElseFactory await(final String alias)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.await(alias);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * A method to increase the readability of the Awaitility DSL. It simply returns the same
   * condition factory instance.
   *
   * @return the condition factory
   */
  public OrElseFactory and()
  {
    return this;
  }

  /**
   * A method to increase the readability of the Awaitility DSL. It simply returns the same
   * condition factory instance.
   *
   * @return the condition factory
   */
  public OrElseFactory with()
  {
    return this;
  }

  /**
   * A method to increase the readability of the Awaitility DSL. It simply returns the same
   * condition factory instance.
   *
   * @return the condition factory
   */
  public OrElseFactory then()
  {
    return this;
  }

  /**
   * A method to increase the readability of the Awaitility DSL. It simply returns the same
   * condition factory instance.
   *
   * @return the condition factory
   */
  public OrElseFactory given()
  {
    return this;
  }

  /**
   * A method to increase the readability of the Awaitility DSL. It simply returns the same
   * condition factory instance.
   *
   * @return the condition factory
   */
  public OrElseFactory but()
  {
    return this;
  }

  /**
   * Don't catch uncaught exceptions in other threads. This will <i>not</i> make the await statement
   * fail if exceptions occur in other threads.
   *
   * @return the condition factory
   */
  @SuppressWarnings("SpellCheckingInspection")
  public OrElseFactory dontCatchUncaughtExceptions()
  {
    final ConditionFactory conditionFactory = this.conditionFactory.dontCatchUncaughtExceptions();
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Specify the executor service whose threads will be used to evaluate the poll condition in
   * Awaitility. Note that the executor service must be shutdown manually!
   * <p>
   * This is an advanced feature and it should only be used sparingly.
   *
   * @param executorService The executor service that Awaitility will use when polling condition
   *                        evaluations
   * @return the condition factory
   */
  public OrElseFactory pollExecutorService(final ExecutorService executorService)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.pollExecutorService(executorService);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Specify a thread supplier whose thread will be used to evaluate the poll condition in
   * Awaitility. The supplier will be called only once and the thread it returns will be reused
   * during all condition evaluations. This is an advanced feature and it should only be used
   * sparingly.
   *
   * @param threadSupplier A supplier of the thread that Awaitility will use when polling
   * @return the condition factory
   */
  public OrElseFactory pollThread(final Function<Runnable, Thread> threadSupplier)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.pollThread(threadSupplier);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Instructs Awaitility to execute the polling of the condition from the same as the test. This is
   * an advanced feature and you should be careful when combining this with conditions that wait
   * forever (or a long time) since Awaitility cannot interrupt the thread when it's using the same
   * thread as the test. For safety you should always combine tests using this feature with a test
   * framework specific timeout, for example in JUnit:
   * <pre>{@code
   * @Test(timeout = 2000L)
   * void myTest() {
   *   Awaitility.pollInSameThread();
   *   await().forever().until(...);
   * }
   * }
   * </pre>
   *
   * @return the condition factory
   */
  public OrElseFactory pollInSameThread()
  {
    final ConditionFactory conditionFactory = this.conditionFactory.pollInSameThread();
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * If the supplied Callable <i>ever</i> returns false, it indicates our condition will
   * <i>never</i> be true, and if so fail the system immediately. Throws a {@link
   * TerminalFailureException} if fail fast condition evaluates to <code>true</code>. If you want to
   * specify a more descriptive error message then use {@link #failFast(String, Callable)}.
   *
   * @param failFastCondition The terminal failure condition
   * @return the condition factory
   * @see #failFast(String, Callable)
   */
  public OrElseFactory failFast(final Callable<Boolean> failFastCondition)
  {
    final ConditionFactory conditionFactory = this.conditionFactory.failFast(failFastCondition);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * If the supplied Callable <i>ever</i> returns false, it indicates our condition will
   * <i>never</i> be true, and if so fail the system immediately. Throws a {@link
   * TerminalFailureException} if fail fast condition evaluates to <code>true</code>.
   *
   * @param failFastCondition     The terminal failure condition
   * @param failFastFailureReason A descriptive reason why the fail fast condition has failed, will
   *                              be included in the {@link TerminalFailureException} thrown if
   *                              <code>failFastCondition</code> evaluates to <code>true</code>.
   * @return the condition factory
   */
  public OrElseFactory failFast(
      final String failFastFailureReason,
      final Callable<Boolean> failFastCondition)
  {
    final ConditionFactory conditionFactory =
        this.conditionFactory.failFast(failFastFailureReason, failFastCondition);
    return new OrElseFactory(conditionFactory, maxAttempts, correctiveAction);
  }

  /**
   * Await until a {@link Callable} supplies a value matching the specified {@link Matcher}. E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().until(numberOfPersons(), is(greaterThan(2)));
   * </pre>
   * <p>&nbsp;</p>
   * where "numberOfPersons()" returns a standard {@link Callable}:
   * <p>&nbsp;</p>
   * <pre>
   * private Callable&lt;Integer&gt; numberOfPersons() {
   * 	return new Callable&lt;Integer&gt;() {
   * 		public Integer call() {
   * 			return personRepository.size();
   *        }
   *    };
   * }
   * </pre>
   * <p>&nbsp;</p>
   * Using a generic {@link Callable} as done by using this version of "until" allows you to reuse
   * the "numberOfPersons()" definition in multiple await statements. I.e. you can easily create
   * another await statement (perhaps in a different test case) using e.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().until(numberOfPersons(), is(equalTo(6)));
   * </pre>
   *
   * @param supplier the supplier that is responsible for getting the value that should be matched.
   * @param matcher  the matcher The hamcrest matcher that checks whether the condition is
   *                 fulfilled.
   * @return a T object.
   * @throws ConditionTimeoutException If condition was not fulfilled within the given time period.
   */
  public <T> T until(final Callable<T> supplier, final Matcher<? super T> matcher)
  {
    verifyNonNull(supplier, "'supplier' cannot be null.");
    verifyNonNull(matcher, "'matcher' cannot be null.");
    final LongAdder counter = new LongAdder();
    final Callable<T> wrappedSupplier =
        CallableWrapper.wrap(supplier, correctiveAction, counter, maxAttempts);
    return this.conditionFactory.until(wrappedSupplier, matcher);
  }

  /**
   * Wait until the given supplier matches the supplied predicate. For example:
   *
   * <pre>
   * await().until(myRepository::count, cnt -> cnt == 2);
   * </pre>
   *
   * @param supplier  The supplier that returns the object that will be evaluated by the predicate.
   * @param predicate The predicate that must match
   * @return a T object.
   * @since 3.1.1
   */
  public <T> T until(final Callable<T> supplier, final Predicate<? super T> predicate)
  {
    verifyNonNull(supplier, "'supplier' cannot be null.");
    verifyNonNull(predicate, "'predicate' cannot be null.");
    final LongAdder counter = new LongAdder();
    final Callable<T> wrappedSupplier =
        CallableWrapper.wrap(supplier, correctiveAction, counter, maxAttempts);
    return this.conditionFactory.until(wrappedSupplier, predicate);
  }

  /**
   * Await until a {@link Runnable} supplier execution passes (ends without throwing an exception).
   * E.g. with Java 8:
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAsserted(() -&gt; Assertions.assertThat(personRepository.size()).isEqualTo(6));
   * </pre>
   * or
   * <pre>
   * await().untilAsserted(() -&gt; assertEquals(6, personRepository.size()));
   * </pre>
   * <p>&nbsp;</p>
   * This method is intended to benefit from lambda expressions introduced in Java 8. It allows to
   * use standard AssertJ/FEST Assert assertions (by the way also standard JUnit/TestNG assertions)
   * to test asynchronous calls and systems.
   * <p>&nbsp;</p>
   * {@link AssertionError} instances thrown by the supplier are treated as an assertion failure and
   * proper error message is propagated on timeout. Other exceptions are rethrown immediately as an
   * execution errors.
   * <p>&nbsp;</p>
   * While technically it is completely valid to use plain Runnable class in Java 7 code, the
   * resulting expression is very verbose and can decrease the readability of the test case, e.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAsserted(new Runnable() {
   *     public void run() {
   *         Assertions.assertThat(personRepository.size()).isEqualTo(6);
   *     }
   * });
   * </pre>
   * <p>&nbsp;</p>
   * <b>NOTE:</b><br>
   * Be <i>VERY</i> careful so that you're not using this method incorrectly in languages (like
   * Kotlin and Groovy) that doesn't disambiguate between a {@link ThrowingRunnable} that doesn't
   * return anything (void) and {@link Callable} that returns a value. For example in Kotlin you can
   * do like this:
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAsserted { true == false }
   * </pre>
   * and the compiler won't complain with an error (as is the case in Java). If you were to execute
   * this test in Kotlin it'll pass!
   *
   * @param assertion the supplier that is responsible for executing the assertion and throwing
   *                  AssertionError on failure.
   * @throws ConditionTimeoutException If condition was not fulfilled within the given time period.
   * @since 1.6.0
   */
  public void untilAsserted(final ThrowingRunnable assertion)
  {
    verifyNonNull(assertion, "'assertion' cannot be null.");
    final LongAdder counter = new LongAdder();
    final ThrowingRunnable wrappedAssertion =
        AssertionWrapper.wrap(assertion, correctiveAction, counter, maxAttempts);
    this.conditionFactory.untilAsserted(wrappedAssertion);
  }

  /**
   * Await until a Atomic variable has a value matching the specified {@link org.hamcrest.Matcher}.
   * E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAtomic(myAtomic, is(greaterThan(2)));
   * </pre>
   *
   * @param atomic  the atomic variable
   * @param matcher the matcher The hamcrest matcher that checks whether the condition is
   *                fulfilled.
   * @return a {@link java.lang.Integer} object.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public Integer untilAtomic(final AtomicInteger atomic, final Matcher<? super Integer> matcher)
  {
    return this.conditionFactory.untilAtomic(atomic, matcher);
  }

  /**
   * Await until a Atomic variable has a value matching the specified {@link org.hamcrest.Matcher}.
   * E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAtomic(myAtomic, is(greaterThan(2)));
   * </pre>
   *
   * @param atomic  the atomic variable
   * @param matcher the matcher The hamcrest matcher that checks whether the condition is
   *                fulfilled.
   * @return a {@link java.lang.Long} object.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public Long untilAtomic(final AtomicLong atomic, final Matcher<? super Long> matcher)
  {
    return this.conditionFactory.untilAtomic(atomic, matcher);
  }

  /**
   * Await until a Atomic variable has a value matching the specified {@link org.hamcrest.Matcher}.
   * E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAtomic(myAtomic, is(greaterThan(2)));
   * </pre>
   *
   * @param atomic  the atomic variable
   * @param matcher the matcher The hamcrest matcher that checks whether the condition is
   *                fulfilled.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilAtomic(final AtomicBoolean atomic, final Matcher<? super Boolean> matcher)
  {
    this.conditionFactory.untilAtomic(atomic, matcher);
  }

  /**
   * Await until a Atomic boolean becomes true.
   *
   * @param atomic the atomic variable
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilTrue(final AtomicBoolean atomic)
  {
    this.conditionFactory.untilTrue(atomic);
  }

  /**
   * Await until a Atomic boolean becomes false.
   *
   * @param atomic the atomic variable
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilFalse(final AtomicBoolean atomic)
  {
    this.conditionFactory.untilFalse(atomic);
  }

  /**
   * Await until a {@link LongAdder} has a value matching the specified {@link
   * org.hamcrest.Matcher}. E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAdder(myLongAdder, is(greaterThan(2L)));
   * </pre>
   *
   * @param adder   the {@link LongAdder} variable
   * @param matcher the matcher The hamcrest matcher that checks whether the condition is
   *                fulfilled.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilAdder(final LongAdder adder, final Matcher<? super Long> matcher)
  {
    this.conditionFactory.untilAdder(adder, matcher);
  }

  /**
   * Await until a {@link DoubleAdder} has a value matching the specified {@link
   * org.hamcrest.Matcher}. E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAdder(myDoubleAdder, is(greaterThan(2.0d)));
   * </pre>
   *
   * @param adder   the {@link DoubleAdder} variable
   * @param matcher the matcher The hamcrest matcher that checks whether the condition is
   *                fulfilled.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilAdder(final DoubleAdder adder, final Matcher<? super Double> matcher)
  {
    this.conditionFactory.untilAdder(adder, matcher);
  }

  /**
   * Await until a {@link LongAccumulator} has a value matching the specified {@link
   * org.hamcrest.Matcher}. E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAccumulator(myLongAccumulator, is(greaterThan(2L)));
   * </pre>
   *
   * @param accumulator the {@link LongAccumulator} variable
   * @param matcher     the matcher The hamcrest matcher that checks whether the condition is
   *                    fulfilled.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilAccumulator(
      final LongAccumulator accumulator,
      final Matcher<? super Long> matcher)
  {
    this.conditionFactory.untilAccumulator(accumulator, matcher);
  }

  /**
   * Await until a {@link DoubleAccumulator} has a value matching the specified {@link
   * org.hamcrest.Matcher}. E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAccumulator(myDoubleAccumulator, is(greaterThan(2.0d)));
   * </pre>
   *
   * @param accumulator the {@link DoubleAccumulator} variable
   * @param matcher     the matcher The hamcrest matcher that checks whether the condition is
   *                    fulfilled.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public void untilAccumulator(
      final DoubleAccumulator accumulator,
      final Matcher<? super Double> matcher)
  {
    this.conditionFactory.untilAccumulator(accumulator, matcher);
  }

  /**
   * Await until a Atomic variable has a value matching the specified {@link org.hamcrest.Matcher}.
   * E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().untilAtomic(myAtomic, is(greaterThan(2)));
   * </pre>
   *
   * @param atomic  the atomic variable
   * @param matcher the matcher The hamcrest matcher that checks whether the condition is
   *                fulfilled.
   * @param <V>     a V object.
   * @return a V object.
   * @throws org.awaitility.core.ConditionTimeoutException If condition was not fulfilled within the
   *                                                       given time period.
   */
  public <V> V untilAtomic(final AtomicReference<V> atomic, final Matcher<? super V> matcher)
  {
    return this.conditionFactory.untilAtomic(atomic, matcher);
  }

  /**
   * Await until a {@link Callable} returns <code>true</code>. This is method is not as generic as
   * the other variants of "until" but it allows for a more precise and in some cases even more
   * english-like syntax. E.g.
   * <p>&nbsp;</p>
   * <pre>
   * await().until(numberOfPersonsIsEqualToThree());
   * </pre>
   * <p>&nbsp;</p>
   * where "numberOfPersonsIsEqualToThree()" returns a standard {@link Callable} of type {@link
   * Boolean}:
   * <p>&nbsp;</p>
   * <pre>
   * private Callable&lt;Boolean&gt; numberOfPersons() {
   * 	return new Callable&lt;Boolean&gt;() {
   * 		public Boolean call() {
   * 			return personRepository.size() == 3;
   *        }
   *    };
   * }
   * </pre>
   *
   * @param conditionEvaluator the condition evaluator
   * @throws ConditionTimeoutException If condition was not fulfilled within the given time period.
   */
  public void until(final Callable<Boolean> conditionEvaluator)
  {
    verifyNonNull(conditionEvaluator, "'conditionEvaluator' cannot be null.");
    final LongAdder counter = new LongAdder();
    final Callable<Boolean> wrappedCondition =
        CallableWrapper.wrap(conditionEvaluator, correctiveAction, counter, maxAttempts);
    this.conditionFactory.until(wrappedCondition);
  }
}
