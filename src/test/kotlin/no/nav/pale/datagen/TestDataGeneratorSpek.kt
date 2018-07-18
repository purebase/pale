package no.nav.pale.datagen

import no.nav.pale.fellesformatJaxBContext
import no.nav.pale.validation.extractIndividualDigits
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.StringWriter
import java.time.LocalDate
import javax.xml.bind.Marshaller

object TestDataGeneratorSpek : Spek({
    // Had an issue with this causing inconsistent test results for integration tests since we were getting person
    // numbers that were generated for the wrong range
    describe("Generating a person born after 2000") {
        val personNumber = generatePersonNumber(LocalDate.of(2005, 1, 1))
        val individualNumber = extractIndividualDigits(personNumber)
        it("Results with a individ number between 500 and 999") {
            individualNumber shouldBeGreaterOrEqualTo 500
            individualNumber shouldBeLessOrEqualTo 999
        }
    }
    describe("Generating a default fellesformat") {
        it("Should not create an exception") {
            val result = StringWriter().let {
                val marshaller = fellesformatJaxBContext.createMarshaller()
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                marshaller.marshal(defaultFellesformat(defaultPerson()), it)
                it.toString()
            }
            println(result)
        }
    }
})
