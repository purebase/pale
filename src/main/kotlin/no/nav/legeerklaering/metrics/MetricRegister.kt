package no.nav.legeerklaering.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge

val RULE_COUNTER: Counter = Counter.Builder().name("rule_counter")
        .labelNames("rule_name")
        .help("Counts the number of times this rule is used").register()

val WS_CALL_TIME: Gauge = Gauge.Builder().name("ws_call_time")
        .labelNames("service")
        .help("Time it takes to execute a soap call").register()
