package no.nav.legeerklaering.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

val RULE_COUNTER: Counter = Counter.Builder().name("rule_counter")
        .labelNames("rule_name")
        .help("Counts the number of times this rule is used").register()

val WS_CALL_TIME: Summary = Summary.Builder().name("ws_call_time")
        .labelNames("service")
        .help("Time it takes to execute a soap call").register()

val INPUT_MESSAGE_TIME: Summary = Summary.build().name("legeerklaering_time")
        .help("Amount of messages received from emottak").register()
