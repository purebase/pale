package no.nav.legeerklaering.metrics

import io.prometheus.client.Collector
import io.prometheus.client.CounterMetricFamily
import org.slf4j.LoggerFactory
import javax.jms.JMSException
import javax.jms.Queue
import javax.jms.Session

private val log = LoggerFactory.getLogger("queue-metrics-collector")
class QueueStatusCollector(val session: Session, vararg val queues: Queue) : Collector() {
    override fun collect(): MutableList<MetricFamilySamples> = mutableListOf(
            CounterMetricFamily("jms_queue_message_counter", "Counts the number of available messages on a queue", listOf("queue_name")).apply {
                for (queue in queues) {
                    try {
                        val browser = session.createBrowser(queue)

                        addMetric(listOf(queue.queueName), browser.enumeration.toList().count().toDouble())
                    } catch (e: JMSException) {
                        log.warn("Failed to collect queue status from {}", queue.queueName, e)
                    }
                }
            }
    )
}
