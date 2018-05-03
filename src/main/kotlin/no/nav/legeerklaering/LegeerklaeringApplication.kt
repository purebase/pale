package no.nav.legeerklaering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import no.nav.legeerklaering.avro.DuplicateCheckedFellesformat
import no.nav.legeerklaering.config.EnvironmentConfig
import no.nav.legeerklaering.validation.OutcomeType
import no.nav.legeerklaering.validation.extractSenderOrganisationNumber
import no.nav.legeerklaering.validation.validatePatientRelations
import no.nav.legeerklaering.validation.validatePersonalInformation
import no.nav.model.arenainfo.ArenaEiaInfo
import no.nav.model.fellesformat.*
import no.nav.model.msghead.*
import no.nav.model.apprec.*
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.fellesregistre.tssws_organisasjon.v3.TsswsOrganisasjonPortType
import no.nav.tjeneste.fellesregistre.tssws_organisasjon.v3.meldinger.HentOrganisasjonRequest
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personidenter
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.*
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.slf4j.LoggerFactory
import java.util.*
import javax.jms.BytesMessage
import javax.jms.MessageConsumer
import javax.xml.datatype.DatatypeFactory
import java.security.MessageDigest
import redis.clients.jedis.Jedis
import java.io.ByteArrayInputStream
import java.math.BigInteger
import javax.xml.bind.JAXBContext


val jaxbAnnotationModule = JaxbAnnotationModule()
val jacksonXmlModule = JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}
//val objectMapper: ObjectMapper = XmlMapper(jacksonXmlModule).registerModule(jaxbAnnotationModule)
//        .enable(SerializationFeature.INDENT_OUTPUT)
val objectMapper: ObjectMapper = ObjectMapper()
val fellesformatJaxBContext = JAXBContext.newInstance(EIFellesformat::class.java, Legeerklaring::class.java)
val fellesformatUnmarshaller = fellesformatJaxBContext.createUnmarshaller()
val newInstance = DatatypeFactory.newInstance()
private val log = LoggerFactory.getLogger(LegeerklaeringApplication::class.java)

class LegeerklaeringApplication {

    fun main(args: Array<String>) {
        val fasitProperties = FasitProperties()

        val connectionFactory = connectionFactory(fasitProperties)

        val environmentConfig = EnvironmentConfig()

        val connection = connectionFactory.createConnection()
        connection.start()

        connection.use {
            queueConnection ->

            Jedis().use {
                jedis ->
                val session = queueConnection.createSession()
                val inputQueue = session.createQueue(fasitProperties.legeerklaeringQueueName)
                val outputQueue = session.createQueue(fasitProperties.arenaQueueName)

                val personV3 = JaxWsProxyFactoryBean().apply {
                    address = environmentConfig.virksomhetPersonV3EndpointURL
                    features.add(LoggingFeature())
                    serviceClass = PersonV3::class.java
                }.create() as PersonV3

                val tssOrganisasjon = JaxWsProxyFactoryBean().apply {
                    address = environmentConfig.tssWSOrganisasjonV4EndpointURL
                    features.add(LoggingFeature())
                    serviceClass = TsswsOrganisasjonPortType::class.java
                } as TsswsOrganisasjonPortType


                val consumer = session.createConsumer(inputQueue)
                listen(consumer, jedis, personV3, tssOrganisasjon)
            }
        }

    }

    fun listen(consumer: MessageConsumer, jedis: Jedis, personV3: PersonV3, tssOrganisasjon: TsswsOrganisasjonPortType) = consumer.setMessageListener {
        if (it is BytesMessage) {
            val bytes = ByteArray(it.bodyLength.toInt())
            it.readBytes(bytes)
            val fellesformat = fellesformatJaxBContext.createUnmarshaller().unmarshal(ByteArrayInputStream(bytes)) as EIFellesformat
            val legeerklaering = fellesformat.msgHead.document[0].refDoc.content.any[0] as Legeerklaring

            val hashValue = createSha256Hash(objectMapper.writeValueAsBytes(legeerklaering))
            if(jedis.exists(hashValue)) {
                createApprec(fellesformat, "duplikat")
            } else {
                jedis.set(hashValue, fellesformat.mottakenhetBlokk.ediLoggId.toString())
            }

            val duplicateCheckedFellesformat = DuplicateCheckedFellesformat().apply {
                setFellesformat(String(bytes, Charsets.UTF_8))
                setRetryCounter(0)
            }

            val outcomes = validateMessage(fellesformat, legeerklaering, personV3)

            val organisation = tssOrganisasjon.hentOrganisasjon(HentOrganisasjonRequest().apply {
                orgnummer = extractSenderOrganisationNumber(fellesformat)
            }).organisasjon
        }
    }

    fun validateMessage(fellesformat: EIFellesformat, legeerklaering: Legeerklaring, personV3: PersonV3): List<OutcomeType> {
        val outcomes = mutableListOf<OutcomeType>()

        val person = try {
            personV3.hentPerson(HentPersonRequest()
                    .withAktoer(PersonIdent().withIdent(
                            NorskIdent()
                                    .withIdent(legeerklaering.pasientopplysninger.pasient.fodselsnummer)
                                    .withType(Personidenter().withValue("FNR")))
                    ).withInformasjonsbehov(Informasjonsbehov.FAMILIERELASJONER)).person
        } catch (e: HentPersonPersonIkkeFunnet) {
            outcomes.add(OutcomeType.PATIENT_NOT_FOUND_IN_TPS)
            return outcomes
        } catch (e: HentPersonSikkerhetsbegrensning) {
            outcomes.add(when (e.faultInfo.sikkerhetsbegrensning[0].value) {
                "FP1_SFA" -> OutcomeType.PATIENT_HAS_SPERREKODE_6
                "FP2_FA" -> OutcomeType.PATIENT_HAS_SPERREKODE_7
                else -> throw RuntimeException("Missing handling of FP3_EA/Egen ansatt")
            })
            return outcomes
        }

        outcomes.addAll(validatePersonalInformation(fellesformat, person))

        val patientRelation = validatePatientRelations(fellesformat, person)
        if (patientRelation != null) {
            outcomes.add(patientRelation)
        }

        return outcomes
    }


    fun createSha256Hash(input: ByteArray): String {
        val bytes = MessageDigest
                .getInstance("SHA1")
                .digest(input)

        return BigInteger(bytes).toString(16)
    }

    fun connectionFactory(fasitProperties: FasitProperties) = MQConnectionFactory().apply {
        hostName = fasitProperties.mqHostname
        port = fasitProperties.mqPort
        queueManager = fasitProperties.mqQueueManagerName
        transportType = WMQConstants.WMQ_CM_CLIENT
        ccsid = 1208
        setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
        setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
    }

    fun createApprec(fellesformat: EIFellesformat, apprecStatus: String): EIFellesformat {

        val fellesformatApprec = EIFellesformat().apply {
            mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
                ediLoggId = fellesformat.mottakenhetBlokk.ediLoggId
                ebRole = LegeerklaeringConstant.ebRoleNav.string
                ebService = LegeerklaeringConstant.ebServiceLegemelding.string
                ebAction = LegeerklaeringConstant.ebActionSvarmelding.string
            }
            appRec = AppRec().apply {
                msgType = AppRecCS().apply {
                    v = LegeerklaeringConstant.APPREC.string
                }
                miGversion = LegeerklaeringConstant.APPRECVersionV1_0.string
                genDate = newInstance.newXMLGregorianCalendar(GregorianCalendar())
                id = fellesformat.mottakenhetBlokk.ediLoggId


                sender = AppRec.Sender().apply {
                    hcp = HCP().apply {
                        inst = Inst().apply {
                            name = fellesformat.msgHead.msgInfo.receiver.organisation.organisationName

                            for (i in fellesformat.msgHead.msgInfo.receiver.organisation.ident.indices) {
                                id = mapIdentToInst(fellesformat.msgHead.msgInfo.receiver.organisation.ident.first()).id
                                typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.receiver.organisation.ident.first()).typeId

                                val additionalIds = fellesformat.msgHead.msgInfo.receiver.organisation.ident.drop(1)
                                        .map { mapIdentToAdditionalId(it) }

                                additionalId.addAll(additionalIds)
                            }
                        }
                    }
                }

                receiver = AppRec.Receiver().apply {
                    hcp = HCP().apply {
                        inst = Inst().apply {
                            name = fellesformat.msgHead.msgInfo.sender.organisation.organisationName

                            for (i in fellesformat.msgHead.msgInfo.sender.organisation.ident.indices) {
                                id = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.ident.first()).id
                                typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.ident.first()).typeId

                                val additionalIds = fellesformat.msgHead.msgInfo.sender.organisation.ident.drop(1)
                                        .map { mapIdentToAdditionalId(it) }

                                additionalId.addAll(additionalIds)
                            }

                            hcPerson.add(HCPerson().apply {
                                name = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                                        fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName

                                for (i in fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.indices) {
                                    id = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first()).id
                                    typeId = mapIdentToInst(fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.first()).typeId

                                    val additionalIds = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident.drop(1)
                                            .map { mapIdentToAdditionalId(it) }

                                    additionalId.addAll(additionalIds)
                                }
                            }
                            )
                        }
                    }

                }

                when {
                    apprecStatus.equals("avvist") -> {
                        status = createApprecStatus(2)
                        error.add(AppRecCV().apply {
                            //@TODO change dn(Get the rule description thas denies the message) and the correct v
                            dn = "LegeErklæringen valideringen misslyktes"
                            v = "47"
                            s = "2.16.578.1.12.4.1.1.8222"
                        })
                    }

                    apprecStatus.equals("duplikat") -> {
                        status = createApprecStatus(2)
                        error.add(AppRecCV().apply {
                            dn = "Duplikat! - Denne meldingen er mottatt tidligere. Skal ikke sendes på nytt."
                            v = "801"
                            s = "2.16.578.1.12.4.1.1.8222"
                        })
                    }


                    apprecStatus.equals("ok") ->
                        status = createApprecStatus(1)

                }

                originalMsgId = OriginalMsgId().apply {
                    msgType = AppRecCS().apply {
                        v = LegeerklaeringConstant.LE.string
                        dn = LegeerklaeringConstant.Legeerklæring.string
                    }
                    issueDate = fellesformat.msgHead.msgInfo.genDate
                    id = fellesformat.msgHead.msgInfo.msgId
                }
            }
        }
        return fellesformatApprec
    }

    fun mapIdentToAdditionalId(ident: Ident): AdditionalId = AdditionalId().apply {
        id = ident.id
        type = AppRecCS().apply {
            dn = ident.typeId.dn
            v = ident.typeId.v
        }
    }

    fun mapIdentToInst(ident: Ident): Inst = Inst().apply {
        id = ident.id
        typeId = AppRecCS().apply {
            dn = ident.typeId.dn
            v = ident.typeId.v
        }
    }

    fun createApprecStatus(status: Int): AppRecCS = AppRecCS().apply {
        if (status == 2) {
            v = "2"
            dn = "Avvist"
        } else {
            v = "1"
            dn = "OK"
        }
    }

    fun checkIfHashValueIsInRedis(jedis: Jedis,hashValue: String): Boolean =
            jedis.get(hashValue) != null

    fun createArenaEiaInfo(legeeklaering: Legeerklaring, fellesformat: EIFellesformat): ArenaEiaInfo = ArenaEiaInfo().apply {
        ediloggId = fellesformat.mottakenhetBlokk.ediLoggId
        hendelseStatus = "TIL_VURDERING" //TODO
        version = "2.0"
        skjemaType = LegeerklaeringConstant.LE.string
        mappeType = "UP"
        pasientData = ArenaEiaInfo.PasientData().apply {
            fnr = legeeklaering.pasientopplysninger.pasient.fodselsnummer
            isSperret = false //TODO
            tkNummer = "" //TODO
        }
        legeData = ArenaEiaInfo.LegeData().apply {
            navn = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                    fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName
            fnr = getHCPFodselsnummer(fellesformat)
            tssid = "asdad" //TODO
        }
        eiaData = ArenaEiaInfo.EiaData().apply {
            systemSvar.add(ArenaEiaInfo.EiaData.SystemSvar().apply {
                meldingsNr = 141.toBigInteger() //TODO
                meldingsTekst = "Usikkert svar fra TSS,  lav sannsynlighet (55,8%) for identifikasjon av  samhandler.  Bør verifiseres." //TODO
                meldingsPrioritet = 4.toBigInteger() //TODO
                meldingsType = "2" //TODO
            })
            signaturDato = newInstance.newXMLGregorianCalendar(GregorianCalendar().apply {
                set(fellesformat.msgHead.msgInfo.genDate.year, fellesformat.msgHead.msgInfo.genDate.month,fellesformat.msgHead.msgInfo.genDate.day)
            })

        }
    }


    fun getHCPFodselsnummer(fellesformat: EIFellesformat): String {

        for (ident in fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.ident) {

            if (ident.typeId.v.equals("FNR"))
            {
                return ident.id
            }

        }

        return ""

    }

    fun tssFinnSamhandlerData(fnr: String): String {
        return ""
    }

    fun tpsFinnPersonData(fnr: String): String {
        return ""
    }

    fun archiveMessage(legeeklaering: Legeerklaring, fellesformat: EIFellesformat):
            LagreDokumentOgOpprettJournalpostRequest = LagreDokumentOgOpprettJournalpostRequest().apply {

        val fagmeldingJournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
            //Fagmelding
            dokumentInfo = DokumentInfo().apply {
                when {
                    legeeklaering.forbeholdLegeerklaring.tilbakeholdInnhold.equals(2.toBigInteger()) ->
                        begrensetPartsinnsynFraTredjePart = false
                    else -> begrensetPartsinnsynFraTredjePart = true
                }
                fildetaljerListe.add(Fildetaljer().apply {
                    //TODO fil = createPDFBase64Encoded(legeeklaering)
                    filnavn = fellesformat.mottakenhetBlokk.ediLoggId+".pdf"
                    filtypeKode = "PDF"
                    variantFormatKode = "ARKIV"
                    versjon = 1
                })
                kategoriKode = "ES"
                tittel = "Legeerklæring"
                brevkode = "900002"
                sensitivt = false
                organInternt = false
                versjon = 1
            }
            tilknyttetJournalpostSomKode = "HOVEDDOKUMENT"
            tilknyttetAvNavn =  "EIA_AUTO"
            versjon = 1
        }

        val behandlingsvedleggJournalpostDokumentInfoRelasjon = JournalpostDokumentInfoRelasjon().apply {
            //Fagmelding
            dokumentInfo = DokumentInfo().apply {
                begrensetPartsinnsynFraTredjePart = legeeklaering.forbeholdLegeerklaring.tilbakeholdInnhold != 2.toBigInteger()

                fildetaljerListe.add(Fildetaljer().apply {
                    //TODO = createPDFBase64Encoded(legeeklaering)
                    filnavn = fellesformat.mottakenhetBlokk.ediLoggId+"-behandlingsvedlegg.pdf"
                    filtypeKode = "PDF"
                    variantFormatKode = "ARKIV"
                    versjon = 1
                })
                kategoriKode = "ES"
                tittel = "Legeerklæring-behandlingsvedlegg"
                brevkode = "900002"
                sensitivt = false
                organInternt = true
                versjon = 1
            }
            tilknyttetJournalpostSomKode = "VEDLEGG"
            tilknyttetAvNavn =  "EIA_AUTO"
            versjon = 1
        }

        journalpostDokumentInfoRelasjonListe.add(fagmeldingJournalpostDokumentInfoRelasjon)
        journalpostDokumentInfoRelasjonListe.add(behandlingsvedleggJournalpostDokumentInfoRelasjon)

        gjelderListe.add(Bruker().apply {
            brukerId = fellesformat.msgHead.msgInfo.patient.ident.get(0).id
            brukertypeKode = "PERSON"
        })
        merknad = "Legeerklæring"
        mottakskanalKode = "EIA"
        mottattDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
        innhold = "Legeerklæring"
        journalForendeEnhetId = ""
        journalposttypeKode = "1"
        journalstatusKode = "MO"
        dokumentDato = newInstance.newXMLGregorianCalendar(GregorianCalendar())
        fagomradeKode = "OPP"
        fordeling = "EIA_OK"
        avsenderMottaker = fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.givenName +
                fellesformat.msgHead.msgInfo.sender.organisation.healthcareProfessional.familyName
        avsenderMottakerId = getHCPFodselsnummer(fellesformat)
        opprettetAvNavn = "EIA_AUTO"

    }

}
