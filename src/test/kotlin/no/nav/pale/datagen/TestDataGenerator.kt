package no.nav.pale.datagen

import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.company.Company
import no.nav.pale.fellesformatJaxBContext
import no.nav.pale.mapping.LegeerklaeringType
import no.nav.pale.mapping.TypeTiltak
import no.nav.pale.validatePersonAndDNumber
import no.nav.pale.validation.personNumberDateFormat
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.pale.*
import no.nav.model.msghead.*
import org.junit.Test
import java.io.StringWriter
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory

val fairy: Fairy = Fairy.create(Locale("no", "NO"))
val random: Random = Random()
val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

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
            marshaller.marshal(defaultFellesformat(), it)
            it.toString()
        }

        println(result)
    }
}

fun defaultFellesformat(): EIFellesformat {
    val person = fairy.person()
    val organisationData = fairy.company()
    val orgAddr = fairy.person().address
    val navAddr = fairy.person().address
    val navOffice = "NAV Oslo"

    val doctor = fairy.person()
    val doctorPersonNumber = generatePersonNumber(doctor.dateOfBirth)

    val patientData = fairy.person()
    val patientPersonNumber = generatePersonNumber(patientData.dateOfBirth)

    return EIFellesformat().apply {
        msgHead = MsgHead().apply {
            msgInfo = MsgInfo().apply {
                type = MsgHeadCS().apply {
                    v = "Legerklæring ved arbeidsuførhet"
                    dn = "LEGEERKL"
                }
                miGversion = "v1.2 2006-05-24"
                genDate = datatypeFactory.newXMLGregorianCalendar()
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
                            givenName = doctor.firstName
                            middleName = doctor.middleName
                            familyName = doctor.lastName
                            ident.add(generateHerIdent())
                            ident.add(generateHPRIdent())
                            ident.add(generatePersonNumberIdent(doctorPersonNumber))
                            address = MsgHeadAddress().apply {
                                streetAdr = doctor.address.addressLine1
                                postalCode = doctor.address.postalCode
                                city = doctor.address.city
                            }
                            teleCom.add(TeleCom().apply {
                                teleAddress = MsgHeadURL().apply {
                                    v = doctor.telephoneNumber
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
                            givenName = patientData.firstName
                            middleName = patientData.middleName
                            familyName = patientData.lastName
                            ident.add(generatePersonNumberIdent(patientPersonNumber))
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
                                        fornavn = patientData.firstName
                                        mellomnavn = patientData.middleName
                                        etternavn = patientData.lastName
                                        fodselsnummer = patientPersonNumber
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
                                                        streetAddress = patientData.address.addressLine1
                                                        postalCode = patientData.address.postalCode
                                                        city = patientData.address.city
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
                                                henvistDato = datatypeFactory.newXMLGregorianCalendar()
                                                antattVentetid = random.nextInt(10).toBigInteger()
                                                spesifikasjon = fairy.textProducer().paragraph()
                                            }
                                        }
                                        diagnoseArbeidsuforhet = DiagnoseArbeidsuforhet().apply {
                                            arbeidsuforFra = datatypeFactory.newXMLGregorianCalendar()
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
                                                    skadeDato = datatypeFactory.newXMLGregorianCalendar()
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
                                                    arbeidssituasjon = 1.toBigInteger() // TODO: 1, 2, 4 and 5
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

                        })


                    }
                }
            })

        }
        mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
            ediLoggId = UUID.randomUUID().toString()
            avsender = doctorPersonNumber
            ebXMLSamtaleId = random.nextInt(999999).toString()
            avsenderRef = random.nextInt(999999).toString()
            avsenderFnrFraDigSignatur = doctorPersonNumber
            mottattDatotid = datatypeFactory.newXMLGregorianCalendar()
            ebRole = "Lege"
            ebService = "Legemelding"
            ebAction = "Legeerklaring"
        }
    }
}

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
    val lookup1: IntArray = intArrayOf(3, 2, 7, 6, 5, 4, 3, 2 )
    if (orgNumber.length != 9)
        return false

    var checksum1 = 0

    for (i in 0..7) {
        val currNum = orgNumber[i]-'0'
        checksum1 += currNum * lookup1[i]
    }

    checksum1 %= 11

    val checksum1Final = if (checksum1 == 0) { 0 } else { 11 - checksum1 }

    if (checksum1Final == 10)
        return false

    return orgNumber[8]-'0' == checksum1Final
}

fun generatePersonNumber(bornDate: LocalDate): String {
    val personDate = bornDate.format(personNumberDateFormat)
    return (11111..99999)
            .map { "$personDate$it" }
            .first {
                validatePersonAndDNumber(it)
            }
}
