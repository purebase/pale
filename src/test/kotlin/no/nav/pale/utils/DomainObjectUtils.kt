package no.nav.pale.utils

import no.nav.model.fellesformat.EIFellesformat
import no.nav.pale.RelationType
import no.nav.pale.validation.extractDoctorIdentFromSender
import no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*

class DomainObjectUtils {
    fun createHentPersonResponse(fellesformat: EIFellesformat,relationType: RelationType):
            HentPersonResponse = HentPersonResponse().apply{
        response.apply {
            person.apply {
                val familierelasjon = familierelasjon( relationType.kodeverkVerdi, fellesformat)
                harFraRolleI.add(familierelasjon)
            }
        }

    }

    fun familierelasjon(faimilierelasjon: String, fellesformat: EIFellesformat): Familierelasjon =  Familierelasjon().apply {
        tilRolle = Familierelasjoner().apply {
            value = faimilierelasjon
        }
        tilPerson = no.nav.tjeneste.virksomhet.person.v3.informasjon.Person().apply {

            aktoer = PersonIdent().apply {
                ident = NorskIdent().apply {
                    val doctorIdent = extractDoctorIdentFromSender(fellesformat)!!
                    ident = doctorIdent.id
                    type = Personidenter().apply {
                        value = doctorIdent.typeId.v
                    }
                }
            }
        }
    }
}