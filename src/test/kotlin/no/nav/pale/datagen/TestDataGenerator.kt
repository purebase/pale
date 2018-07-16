package no.nav.pale.datagen

import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.company.Company
import com.devskiller.jfairy.producer.person.PersonProperties
import com.devskiller.jfairy.producer.person.PersonProvider
import io.ktor.util.toLocalDateTime
import no.nav.pale.fellesformatJaxBContext
import no.nav.pale.mapping.LegeerklaeringType
import no.nav.pale.mapping.TypeTiltak
import no.nav.pale.validation.validatePersonAndDNumber
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.msghead.Document
import no.nav.model.msghead.HealthcareProfessional
import no.nav.model.msghead.Ident
import no.nav.model.msghead.MsgHead
import no.nav.model.msghead.MsgHeadAddress
import no.nav.model.msghead.MsgHeadCS
import no.nav.model.msghead.MsgHeadCV
import no.nav.model.msghead.MsgHeadURL
import no.nav.model.msghead.MsgInfo
import no.nav.model.msghead.Organisation
import no.nav.model.msghead.Patient
import no.nav.model.msghead.Receiver
import no.nav.model.msghead.RefDoc
import no.nav.model.msghead.Sender
import no.nav.model.msghead.TeleCom
import no.nav.model.pale.AktueltTiltak
import no.nav.model.pale.AndreOpplysninger
import no.nav.model.pale.Arbeidsforhold
import no.nav.model.pale.Arbeidssituasjon
import no.nav.model.pale.DiagnoseArbeidsuforhet
import no.nav.model.pale.DiagnoseKodesystem
import no.nav.model.pale.Enkeltdiagnose
import no.nav.model.pale.ForbeholdLegeerklaring
import no.nav.model.pale.ForslagTiltak
import no.nav.model.pale.HenvistUtredning
import no.nav.model.pale.Kontakt
import no.nav.model.pale.Legeerklaring
import no.nav.model.pale.LegeerklaringGjelder
import no.nav.model.pale.Pasient
import no.nav.model.pale.Pasientopplysninger
import no.nav.model.pale.PlanUtredBehandle
import no.nav.model.pale.Prognose
import no.nav.model.pale.Teleinformasjon
import no.nav.model.pale.TypeAdresse
import no.nav.model.pale.TypeAdressetype
import no.nav.model.pale.TypeNavn
import no.nav.model.pale.TypePostalAddress
import no.nav.model.pale.TypeTelekomtype
import no.nav.model.pale.Virksomhet
import no.nav.model.pale.VurderingArbeidsevne
import no.nav.model.pale.VurderingFunksjonsevne
import no.nav.model.pale.VurderingYrkesskade
import no.nav.pale.client.Samhandler
import no.nav.pale.client.SamhandlerDirekteOppgjoerAvtale
import no.nav.pale.client.SamhandlerIdent
import no.nav.pale.client.SamhandlerPeriode
import no.nav.pale.client.SamhandlerPraksis
import no.nav.pale.client.SamhandlerPraksisEmail
import no.nav.pale.client.SamhandlerPraksisKonto
import no.nav.pale.mapping.KontaktType
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Enhetsstatus
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.informasjon.Organisasjonsenhet
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadresse
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer
import no.nav.tjeneste.virksomhet.person.v3.informasjon.UstrukturertAdresse
import org.junit.Test
import java.io.StringWriter
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.Random
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory

val fairy: Fairy = Fairy.create(Locale("no", "NO"))
val random: Random = Random()
val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()
val personNumberDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")

val defaultPersonProperties = arrayOf(
        PersonProperties.ageBetween(PersonProvider.MIN_AGE, 69)
)

fun Company.getCenterName(): String =
        this.name.replace("AS", when (this.name.length % 3) {
            0 -> "Legesenter AS"
            1 -> "Legekontor AS"
            else -> "AS"
        })

class TestDataGenerator {
    @Test
    fun generateTestFellesformat() {
        val result = StringWriter().let {
            val marshaller = fellesformatJaxBContext.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            marshaller.marshal(defaultFellesformat(defaultPerson()), it)
            it.toString()
        }

        println(result)
    }
}

class GeneratedAddress(var city: String? = null) : Gateadresse()

fun Person.toSamhandler(): Samhandler {
    val personNumber = ident()
    val random = Random(personNumber.toLong())
    val samhandlerIdent = random.nextInt(1000000000) + 1000000000
    val samhandlerIdentId = random.nextInt(1000000000) + 1000000000
    val samhandlerPraksisId = random.nextInt(1000000000) + 1000000000
    return Samhandler(
            samh_id = samhandlerIdent.toString(),
            navn = this.personnavn.sammensattNavn,
            samh_type_kode = "LE",
            behandling_utfall_kode = "auto",
            unntatt_veiledning = "1",
            godkjent_manuell_krav = "0",
            ikke_godkjent_for_refusjon = "0",
            godkjent_egenandel_refusjon = "0",
            godkjent_for_fil = "0",
            endringslogg_tidspunkt_siste = LocalDateTime.now().minusDays(14),
            samh_praksis = listOf(
                    SamhandlerPraksis(
                            navn = "Legesenter 12",
                            refusjon_type_kode = "normal_refusjon",
                            laerer = "0",
                            lege_i_spesialisering = "0",
                            tidspunkt_resync_periode = LocalDateTime.now().minusYears(1),
                            tidspunkt_registrert = LocalDateTime.now().minusYears(5),
                            samh_praksis_status_kode = "aktiv",
                            telefonnr = telephoneNumber(this).toString(),
                            arbeids_kommune_nr = "0123",
                            arbeids_postnr = "0123",
                            arbeids_adresse_linje_1 = "Oppdiktet gate 123",
                            arbeids_adresse_linje_2 = null,
                            arbeids_adresse_linje_3 = null,
                            arbeids_adresse_linje_4 = null,
                            arbeids_adresse_linje_5 = null,
                            tss_ident = (random.nextInt(1000000000) + 1000000000).toString(),
                            samh_praksis_type_kode = "FALE",
                            samh_id = samhandlerIdent.toString(),
                            samh_praksis_id = samhandlerPraksisId.toString(),
                            samh_praksis_konto = listOf(SamhandlerPraksisKonto(
                                    tidspunkt_registrert = LocalDateTime.now().minusYears(5),
                                    registrert_av_id = "AB123CDE",
                                    konto = "12341212345",
                                    samh_praksis_id = samhandlerPraksisId.toString(),
                                    samh_praksis_konto_id = (random.nextInt(1000000000) + 1000000000).toString()
                            )),
                            samh_praksis_periode = listOf(SamhandlerPeriode(
                                    endret_ved_import = "0",
                                    sist_endret = LocalDateTime.now().minusYears(5),
                                    slettet = "0",
                                    gyldig_fra = Date(0).toLocalDateTime(),
                                    gyldig_til = null,
                                    samh_praksis_id = samhandlerPraksisId.toString(),
                                    samh_praksis_periode_id = (random.nextInt(1000000000) + 1000000000).toString()
                            )),
                            samh_praksis_email = listOf(SamhandlerPraksisEmail(
                                    samh_praksis_email_id = (random.nextInt(1000000000) + 1000000000).toString(),
                                    samh_praksis_id = samhandlerPraksisId.toString(),
                                    email = "void@dev.null",
                                    primaer_email = null
                            )),

                            post_postnr = null,
                            post_kommune_nr = null,
                            post_adresse_linje_1 = null,
                            post_adresse_linje_2 = null,
                            post_adresse_linje_3 = null,
                            post_adresse_linje_4 = null,
                            post_adresse_linje_5 = null,
                            her_id = null,
                            resh_id = null,
                            ident = "2"
                    )
            ),
            breg_hovedenhet = null,
            samh_ident = listOf(
                    SamhandlerIdent(
                            samh_id = samhandlerIdent.toString(),
                            samh_ident_id = samhandlerIdentId.toString(),
                            ident = ident(),
                            ident_type_kode = "FNR",
                            aktiv_ident = "1"
                    ),
                    SamhandlerIdent(
                            samh_id = samhandlerIdent.toString(),
                            samh_ident_id = samhandlerIdentId.toString(),
                            ident = "1234567",
                            ident_type_kode = "HPR",
                            aktiv_ident = "1"
                    )),
            samh_avtale = listOf(),
            samh_email = listOf(),
            samh_direkte_oppgjor_avtale = listOf(SamhandlerDirekteOppgjoerAvtale(
                    gyldig_fra = LocalDateTime.now().minusYears(15),
                    samh_id = samhandlerIdent.toString(),
                    samh_direkte_oppgjor_avtale_id = "1000050000",
                    koll_avtale_mottatt_dato = null,
                    monster_avtale_mottatt_dato = null
            ))
    )
}

fun defaultNavOffice(): Organisasjonsenhet = Organisasjonsenhet().apply {
    enhetNavn = "NAV Sverige"
    enhetId = "nav123"
    organisasjonsnummer = generateOrganisationNumber()
    status = Enhetsstatus.AKTIV
}

fun telephoneNumber(person: Person): Int = Random(person.ident().toLongOrNull() ?: 0)
            .nextInt(10000000) + 90000000

fun createFamilyRelation(type: String?, other: Person): Familierelasjon =
        Familierelasjon()
                .withTilRolle(Familierelasjoner().withValue(type))
                .withTilPerson(other)

fun defaultPerson(
    vararg personProperties: PersonProperties.PersonProperty = defaultPersonProperties,
    familyRelations: Array<Familierelasjon> = arrayOf(),
    useDNumber: Boolean = false
): Person {
    val person = fairy.person(*personProperties)
    return Person()
            .withAktoer(PersonIdent().withIdent(NorskIdent()
                    .withType(Personidenter().withValue("FNR"))
                    .withIdent(generatePersonNumber(person.dateOfBirth, useDNumber))))
            .withPersonnavn(Personnavn()
                    .withFornavn(person.firstName)
                    .withMellomnavn(person.middleName)
                    .withEtternavn(person.lastName)
                    .withSammensattNavn("${person.firstName} ${person.middleName} ${person.lastName}"))
            .withBostedsadresse(Bostedsadresse()
                    .withStrukturertAdresse(GeneratedAddress(person.address.city)
                            .withLandkode(Landkoder().withValue("NO"))
                            .withBolignummer(person.address.streetNumber)
                            .withPoststed(Postnummer().withValue(person.address.postalCode))
                            .withGatenavn(person.address.street)
                            .withHusnummer(person.address.apartmentNumber.toIntOrNull())
                    )
                    .withEndringstidspunkt(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now().minusYears(1)))))
            .withPostadresse(Postadresse()
                    .withUstrukturertAdresse(UstrukturertAdresse()
                            .withAdresselinje1(person.address.addressLine1)
                            .withAdresselinje2(person.address.addressLine2))
                    .withEndringstidspunkt(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now().minusYears(1)))))
            .withHarFraRolleI(*familyRelations)
}

fun defaultFellesformat(
    person: Person,
    doctor: Person = defaultPerson(),
    doctorTelephoneNumber: Int = telephoneNumber(doctor),
    signatureDate: ZonedDateTime = ZonedDateTime.now().minusDays(1),
    receivedDate: ZonedDateTime = ZonedDateTime.now()
): EIFellesformat {
    val organisationData = fairy.company()
    val orgAddr = fairy.person().address
    val navAddr = fairy.person().address
    val navOffice = "NAV Oslo"

    return EIFellesformat().apply {
        msgHead = MsgHead().apply {
            msgInfo = MsgInfo().apply {
                type = MsgHeadCS().apply {
                    v = "Legerklæring ved arbeidsuførhet"
                    dn = "LEGEERKL"
                }
                miGversion = "v1.2 2006-05-24"
                genDate = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(signatureDate))
                msgId = UUID.randomUUID().toString()
                sender = Sender().apply {
                    organisation = Organisation().apply {
                        organisationName = organisationData.getCenterName()
                        ident.add(generateOrganisationNumberIdent())
                        ident.add(generateHerIdent())

                        address = MsgHeadAddress().apply {
                            streetAdr = orgAddr.addressLine1
                            postalCode = orgAddr.postalCode
                            city = orgAddr.city
                        }
                        healthcareProfessional = HealthcareProfessional().apply {
                            givenName = doctor.personnavn.fornavn
                            middleName = doctor.personnavn.mellomnavn
                            familyName = doctor.personnavn.etternavn
                            ident.add(generateHerIdent())
                            ident.add(generateHPRIdent())
                            ident.add(generatePersonNumberIdent(doctor.ident()))
                            address = MsgHeadAddress().apply {
                                val generatedAddress = doctor.bostedsadresse.strukturertAdresse as GeneratedAddress
                                streetAdr = doctor.postadresse.ustrukturertAdresse.adresselinje1
                                postalCode = generatedAddress.poststed.value
                                city = generatedAddress.city
                            }
                            teleCom.add(TeleCom().apply {
                                teleAddress = MsgHeadURL().apply {
                                    v = doctorTelephoneNumber.toString()
                                }
                                typeTelecom = MsgHeadCS().apply {
                                    dn = "Hovedtelefon"
                                }
                            })
                        }
                        receiver = Receiver().apply {
                            organisation = Organisation().apply {
                                organisationName = "NAV"
                                ident.add(generateOrganisationNumberIdent())
                                ident.add(generateHerIdent())
                                address = MsgHeadAddress().apply {
                                    streetAdr = navAddr.addressLine1
                                    postalCode = navAddr.postalCode
                                    city = navAddr.city
                                }
                            }
                        }
                        patient = Patient().apply {
                            givenName = person.personnavn.fornavn
                            middleName = person.personnavn.mellomnavn
                            familyName = person.personnavn.etternavn
                            val personIdent = person.identAllowNull()
                            if (personIdent != null)
                                ident.add(generatePersonNumberIdent(personIdent))
                        }
                    }
                }
            }
            document.add(Document().apply {
                refDoc = RefDoc().apply {
                    content = RefDoc.Content().apply {
                        any.add(Legeerklaring().apply {
                            val workplace = fairy.person()
                            legeerklaringGjelder.add(LegeerklaringGjelder().apply {
                                typeLegeerklaring = generateLegeerklaeringType().toBigInteger()
                            })
                            pasientopplysninger = Pasientopplysninger().apply {
                                flereArbeidsforhold = ThreadLocalRandom.current().nextInt(0, 2 + 1).toBigInteger()
                                pasient = Pasient().apply {
                                    navn = TypeNavn().apply {
                                        fornavn = person.personnavn.fornavn
                                        mellomnavn = person.personnavn.mellomnavn
                                        etternavn = person.personnavn.etternavn
                                        fodselsnummer = person.identAllowNull()
                                        trygdekontor = navOffice
                                    }
                                    arbeidsforhold = Arbeidsforhold().apply {
                                        primartArbeidsforhold = 1.toBigInteger()
                                        virksomhet = Virksomhet().apply {
                                            virksomhetsAdr = TypeAdresse().apply {
                                                adressetype = TypeAdressetype.RES
                                                postalAddress.add(TypePostalAddress().apply {
                                                    streetAddress = workplace.address.addressLine1
                                                    postalCode = workplace.address.postalCode
                                                    city = workplace.address.city
                                                    country = "Norge"
                                                })
                                                teleinformasjon.add(Teleinformasjon().apply {
                                                    typeTelekom = TypeTelekomtype.MC
                                                    value = workplace.telephoneNumber
                                                })
                                                yrkesbetegnelse = "Dokumentforfalsker"
                                                personAdr.add(TypeAdresse().apply {
                                                    adressetype = TypeAdressetype.H
                                                    postalAddress.add(TypePostalAddress().apply {
                                                        val generatedAddress = doctor.bostedsadresse.strukturertAdresse as GeneratedAddress
                                                        streetAddress = doctor.postadresse.ustrukturertAdresse.adresselinje1
                                                        postalCode = generatedAddress.poststed.value
                                                        city = generatedAddress.city
                                                        country = "Norway"
                                                    })
                                                })
                                            }
                                            virksomhetsBetegnelse = workplace.company.name
                                        }
                                        planUtredBehandle = PlanUtredBehandle().apply {
                                            nyVurdering = fairy.textProducer().paragraph()
                                            behandlingsPlan = fairy.textProducer().paragraph()
                                            utredningsPlan = fairy.textProducer().paragraph()
                                            nyeLegeopplysninger = fairy.textProducer().paragraph()
                                            henvistUtredning = HenvistUtredning().apply {
                                                henvistDato = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()))
                                                antattVentetid = random.nextInt(10).toBigInteger()
                                                spesifikasjon = fairy.textProducer().paragraph()
                                            }
                                        }
                                        diagnoseArbeidsuforhet = DiagnoseArbeidsuforhet().apply {
                                            arbeidsuforFra = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()))
                                            diagnoseKodesystem = DiagnoseKodesystem().apply {
                                                kodesystem = ThreadLocalRandom.current().nextInt(0, 2 + 1).toBigInteger()
                                                enkeltdiagnose.add(Enkeltdiagnose().apply {
                                                    diagnose = "82-01-Le"
                                                    kodeverdi = "K74"
                                                    sortering = 0.toBigInteger()
                                                })
                                                enkeltdiagnose.add(Enkeltdiagnose().apply {
                                                    diagnose = "Nyresvikt kronisk"
                                                    kodeverdi = "U99"
                                                    sortering = 1.toBigInteger()
                                                })
                                                vurderingYrkesskade = VurderingYrkesskade().apply {
                                                    borVurderes = random.nextBoolean().toBigInteger()
                                                    skadeDato = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()))
                                                }
                                                statusPresens = fairy.textProducer().paragraph()
                                                symptomerBehandling = fairy.textProducer().paragraph()
                                            }
                                            forslagTiltak = ForslagTiltak().apply {
                                                aktueltTiltak.addAll(generateTypeTiltak())
                                                opplysninger = fairy.textProducer().paragraph()
                                                begrensningerTiltak = fairy.textProducer().paragraph()
                                            }
                                            vurderingFunksjonsevne = VurderingFunksjonsevne().apply {
                                                arbeidssituasjon += Arbeidssituasjon().apply {
                                                    vurderingArbeidsevne = VurderingArbeidsevne().apply {
                                                        gjenopptaArbeid = random.nextBoolean().toBigInteger()
                                                        taAnnetArbeid = random.nextBoolean().toBigInteger()
                                                        narTaAnnetArbeid = random.nextBoolean().toBigInteger()
                                                        narGjenopptaArbeid = random.nextBoolean().toBigInteger()
                                                        ikkeGjore = fairy.textProducer().paragraph()
                                                        hensynAnnetYrke = fairy.textProducer().paragraph()
                                                    }
                                                    kravArbeid = fairy.textProducer().paragraph()
                                                    val arbeidssituasjonList = listOf(1, 2, 4, 5)
                                                    arbeidssituasjon = arbeidssituasjonList.randomElement()?.toBigInteger()
                                                    funksjonsevne = fairy.textProducer().paragraph()
                                                }
                                                prognose = Prognose().apply {
                                                    bedreArbeidsevne = random.nextBoolean().toBigInteger()
                                                    varighetFunksjonsnedsettelse = fairy.textProducer().paragraph()
                                                    varighetNedsattArbeidsevne = fairy.textProducer().paragraph()
                                                    antattVarighet = Random().nextInt(10).toString()
                                                }
                                                andreOpplysninger = AndreOpplysninger().apply {
                                                    onskesKopi = random.nextBoolean().toBigInteger()
                                                    opplysning = fairy.textProducer().paragraph()
                                                }
                                                forbeholdLegeerklaring = ForbeholdLegeerklaring().apply {
                                                    tilbakeholdInnhold = random.nextBoolean().toBigInteger()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            arsakssammenhengLegeerklaring = fairy.textProducer().paragraph()
                            val values = KontaktType.values().toMutableList()
                            kontakt.addAll((0..random.nextInt(values.size - 1)).map {
                                Kontakt().apply {
                                    kontakt = values.removeAt(random.nextInt(values.size)).type.toBigInteger()
                                    if (kontakt.toInt() == KontaktType.AnnenInstans.type) {
                                        annenInstans = "Kongen"
                                    }
                                }
                            })
                        })
                    }
                }
            })
        }
        mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
            ediLoggId = UUID.randomUUID().toString()
            avsender = doctor.ident()
            ebXMLSamtaleId = random.nextInt(999999).toString()
            avsenderRef = random.nextInt(999999).toString()
            avsenderFnrFraDigSignatur = doctor.ident()
            mottattDatotid = datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(receivedDate))
            ebRole = "Lege"
            ebService = "Legemelding"
            ebAction = "Legeerklaring"
        }
    }
}

fun Person.identAllowNull(): String? = (this.aktoer as PersonIdent).ident.ident
fun Person.ident(): String = identAllowNull()!!

fun Boolean.toBigInteger(): BigInteger =
        if (this) {
            1
        } else {
            2
        }.toBigInteger()

fun generateTypeTiltak(): List<AktueltTiltak> = TypeTiltak.values()
        .filter { random.nextInt(3) == 1 }
        .map { AktueltTiltak().apply { typeTiltak = it.typeTiltak.toBigInteger() } }

fun generateLegeerklaeringType(): Int =
        LegeerklaeringType.values()[random.nextInt(LegeerklaeringType.values().size)].type

fun generatePersonNumberIdent(personNumber: String): Ident = Ident().apply {
    id = personNumber
    typeId = MsgHeadCV().apply {
        dn = "Fødselsnummer"
        v = "FNR"
        s = "6.87.654.3.21.9.8.7.6543.2198"
    }
}

fun generateHerIdent(): Ident = Ident().apply {
    id = random.nextInt(9999999).toString()
    typeId = MsgHeadCV().apply {
        dn = "Identifikator fra Helsetjenesteenhetsregisteret"
        v = "HER"
        s = "1.23.456.7.89.1.2.3.4567.8912"
    }
}

fun generateHPRIdent(): Ident = Ident().apply {
    id = random.nextInt(9999999).toString()
    typeId = MsgHeadCV().apply {
        dn = "HPR-nummer"
        v = "HPR"
        s = "6.87.654.3.21.9.8.7.6543.2198"
    }
}

fun generateOrganisationNumberIdent(): Ident = Ident().apply {
    id = generateOrganisationNumber()
    typeId = MsgHeadCV().apply {
        dn = "Organisasjonsnummeret i Enhetsregister (Brønnøysund)"
        v = "ENH"
        s = "1.16.578.1.12.3.1.1.9051"
    }
}

fun generateOrganisationNumber(): String {
    return (999999900..999999999)
            .map { "$it" }
            .first {
                validateOrgNumberMod11(it)
            }
}

private fun validateOrgNumberMod11(orgNumber: String): Boolean {
    val lookup1: IntArray = intArrayOf(3, 2, 7, 6, 5, 4, 3, 2)
    if (orgNumber.length != 9)
        return false

    var checksum1 = 0

    for (i in 0..7) {
        val currNum = orgNumber[i] - '0'
        checksum1 += currNum * lookup1[i]
    }

    checksum1 %= 11

    val checksum1Final = if (checksum1 == 0) { 0 } else { 11 - checksum1 }

    if (checksum1Final == 10)
        return false

    return orgNumber[8] - '0' == checksum1Final
}

fun generatePersonNumber(bornDate: LocalDate, useDNumber: Boolean = false): String {
    val personDate = bornDate.format(personNumberDateFormat).let {
        if (useDNumber) "${it[0] + 4}${it.substring(1)}" else it
    }
    return (if (bornDate.year > 2000) (75011..99999) else (11111..50099))
            .map { "$personDate$it" }
            .first {
                validatePersonAndDNumber(it)
            }
}
fun List<Int>.randomElement() =
        if (this.isEmpty()) null else this[Random().nextInt(this.size)]
