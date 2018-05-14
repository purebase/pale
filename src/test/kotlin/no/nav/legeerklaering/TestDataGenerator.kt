package no.nav.legeerklaering

import com.devskiller.jfairy.Fairy
import no.nav.legeerklaering.mapping.LegeerklaeringType
import no.nav.legeerklaering.mapping.TypeTiltak
import no.nav.legeerklaering.validation.personNumberDateFormat
import no.nav.model.fellesformat.EIFellesformat
import no.nav.model.legeerklaering.*
import no.nav.model.msghead.*
import org.junit.Test
import java.io.StringWriter
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import javax.xml.bind.Marshaller
import javax.xml.datatype.DatatypeFactory

val fairy: Fairy = Fairy.create(Locale.US)
val random: Random = Random()
val datatypeFactory: DatatypeFactory = DatatypeFactory.newInstance()

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
                        organisationName = organisationData.name
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
                content = EIFellesformat.Content().apply {
                    any.add(Legeerklaring().apply {
                        val workplace = fairy.person()
                        legeerklaringGjelder.add(LegeerklaringGjelder().apply {
                            typeLegeerklaring = generateLegeerklaeringType().toBigInteger()
                        })
                        pasientopplysninger = Pasientopplysninger().apply {
                            flereArbeidsforhold = 2.toBigInteger() // TODO
                            pasient = Pasient().apply {
                                navn = TypeNavn().apply {
                                    fornavn = patientData.firstName
                                    mellomnavn = patientData.middleName
                                    etternavn = patientData.lastName
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
                                            kodesystem = 1.toBigInteger() // TODO
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
                                                funksjonsevne = fairy.textProducer().paragraph()
                                            }
                                            prognose = Prognose().apply {
                                                bedreArbeidsevne = random.nextBoolean().toBigInteger()
                                                varighetFunksjonsnedsettelse = fairy.textProducer().paragraph()
                                                varighetNedsattArbeidsevne = fairy.textProducer().paragraph()
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
                    })
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

fun generateOrganisationNumber(): String = random.nextInt(9999999).toString() // TODO: Generate a organization with a valid mod

fun generatePersonNumber(bornDate: LocalDate): String {
    val personDate = bornDate.format(personNumberDateFormat)
    return (11111..99999)
            .map { "$personDate$it" }
            .first {
                validatePersonAndDNumber(it)
            }
}
