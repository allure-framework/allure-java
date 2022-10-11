## Allure-awaitility
Extended logging for poling and ignored exceptions for [awaitility](https://github.com/awaitility/awaitility)


## Wiki
For more information about awaitility highly recommended look into [awaitility usage guide](https://github.com/awaitility/awaitility/wiki/Usage)


### Configuration examples
Single line for all awaitility conditions in project
```java
Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener());
```

Moreover, it's possible logging only few unstable conditions with method `.conditionEvaluationListener()`
```java
final AtomicInteger atomicInteger = new AtomicInteger(0);
await().with()
        .conditionEvaluationListener(new AllureAwaitilityListener())
        .alias("Checking that important counter reached value around 3")
        .atMost(Duration.of(1000, ChronoUnit.MILLIS))
        .pollInterval(Duration.of(50, ChronoUnit.MILLIS))
        .until(atomicInteger::getAndIncrement, is(3));
```

How it looks like:
1. Top-level step with condition evaluation definition such as alias or default information.
2. Second-level steps with poling process information
3. Optional second-level step with timeout information
4. Optional second-level steps with ignored information


### TimeUnit
Most awaitility users count time as milliseconds, but you can feel free to change print poll information.
```java
Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener().setUnit(TimeUnit.SECONDS));
```


### Exceptions handling
By default, it's not possible to handle and log any exceptions, but you can try to 
[ignore](https://github.com/awaitility/awaitility/wiki/Usage#ignoring-exceptions) and log ignored exceptions. 

```java
final AtomicInteger atomicInteger = new AtomicInteger(0);
await().with()
        .ignoreExceptions() //required
        .atMost(Duration.of(1000, ChronoUnit.MILLIS))
        .pollInterval(Duration.of(500, ChronoUnit.MILLIS))
        .until(() -> {
            if (atomicInteger.getAndIncrement() != 3) {
                //this exception will be ignored by awaitility, but logged into Allure
                throw new RuntimeException("Something wrong happens");
            } else {
                return true;
            }
        });
```

Then, if you are not impressed with the large volume of logged exceptions, there is the way to disable logging for 
ignored exceptions globally.

```java
Awaitility.ignoreExceptionsByDefault();
Awaitility.setDefaultConditionEvaluationListener(new AllureAwaitilityListener().setLogIgnoredExceptions(false));
```