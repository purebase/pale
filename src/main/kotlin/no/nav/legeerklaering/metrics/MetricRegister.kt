package no.nav.legeerklaering.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val METRICS_NS = "bankkontonummerkanal"

val RULE_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("rule_counter")
        .labelNames("rule_name")
        .help("Counts the number of times this rule is used").register()

val WS_CALL_TIME: Summary = Summary.Builder()
        .namespace(METRICS_NS)
        .name("ws_call_time")
        .labelNames("service")
        .help("Time it takes to execute a soap call").register()

val INPUT_MESSAGE_TIME: Summary = Summary.build()
        .namespace(METRICS_NS)
        .name("legeerklaering_time")
        .help("Amount of messages received from emottak").register()

val APPREC_STATUS_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("apprec_status_counter")
        .labelNames("apprec_status_type")
        .help("Counts the number of apprec status types we send back to emottak").register()

val APPREC_ERROR_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("apprec_error_counter")
        .labelNames("apprec_error_type")
        .help("Counts the number of apprec error we send back to emottak").register()
