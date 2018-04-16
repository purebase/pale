package no.nav.legeerklaering

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import com.ibm.msg.client.wmq.compat.base.internal.MQC
import no.nav.model.arenaEiaSkjema.ArenaEiaInfo
import no.nav.model.fellesformat.*
import no.nav.model.legeerklaering.Legeerklaring
import no.nav.tjeneste.virksomhet.person.v3.HentPerson
import no.nav.virksomhet.tjenester.arkiv.journalbehandling.meldinger.v1.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.slf4j.LoggerFactory
import java.util.*
import javax.jms.BytesMessage
import javax.jms.MessageConsumer
import javax.xml.datatype.DatatypeFactory
import java.security.MessageDigest
import redis.clients.jedis.Jedis
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.xml.datatype.XMLGregorianCalendar


val jaxbAnnotationModule = JaxbAnnotationModule()
val jacksonXmlModule = JacksonXmlModule().apply {
    setDefaultUseWrapper(false)
}
val objectMapper: ObjectMapper = XmlMapper(jacksonXmlModule).registerModule(jaxbAnnotationModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)
val newInstance = DatatypeFactory.newInstance()
private val log = LoggerFactory.getLogger(LegeerklaeringApplication::class.java)

class LegeerklaeringApplication {

    fun main(args: Array<String>) {
        val fasitProperties = FasitProperties()

        val connectionFactory = connectionFactory(fasitProperties)

        val connection = connectionFactory.createConnection()
        connection.start()

        connection.use {
            val session = it.createSession()
            val inputQueue = session.createQueue(fasitProperties.legeerklaeringQueueName)
            val outputQueue = session.createQueue(fasitProperties.arenaQueueName)
            val hentPerson = HentPerson()

            val consumer = session.createConsumer(inputQueue)
            listen(consumer)
        }

    }

    fun listen(consumer: MessageConsumer) = consumer.setMessageListener {
        if (it is BytesMessage) {
            val bytes = ByteArray(it.bodyLength.toInt())
            it.readBytes(bytes)
            val fellesformat = objectMapper.readValue<EIFellesformat>(bytes, EIFellesformat::class.java)

            val hashValue = createSHA1(fellesformat.toString())

            val jedis = Jedis("localhost", 6379)
            log.debug("Connection to reids server sucessfully")
            log.debug("Redis server is running: " + jedis.ping())

            val duplicate = checkIfHashValueIsInRedis(jedis,hashValue)

            if(duplicate) {
                createApprec(fellesformat, "duplikat")
            }
            else
            {
                jedis.set(hashValue, fellesformat.mottakenhetBlokk.ediLoggId.toString())
            }

            val legeerklaering = fellesformat.msgHead.document.get(0).refDoc.content.any as Legeerklaring
            val outcome = validateMessage(legeerklaering)

            when (outcome) {
                is Success -> {
                    // Send message to a internal kafka topic

                    //poll it message from kafka topic

                    //call TPS
                    tpsFinnPersonData("fnr")

                    //call TSS
                    tssFinnSamhandlerData("fnr")
                    //Run the rule controll

                    //Akriver Message
                    archiveMessage(legeerklaering,fellesformat)

                    // send to Message
                    createArenaEiaInfo(legeerklaering, fellesformat)
                }

                is ValidationError -> {
                    createApprec(fellesformat, "avvist")
                }
            }
        }
    }

    fun validateMessage(legeeklaering: Legeerklaring): Outcome {

        //TODO need to implement fpsak|-nare to implments rules

        val pasient = legeeklaering.pasientopplysninger.pasient
        if (!validatePersonNumber(pasient.fodselsnummer)) {
            ValidationError("Invalid personnummer, checksum not matching")
        }

        return Success()
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


    fun createSHA1(input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance("sha1")
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }

    fun checkIfHashValueIsInRedis(jedis: Jedis,hashValue: String): Boolean {

        if (jedis.get(hashValue) == null) {
            log.info("hashValue is not in redis")
            return false
        } else {
            log.info("hashValue is  in redis")
            return true

        }
    }

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

    fun archiveMessage(legeeklaering: Legeerklaring ,fellesformat: EIFellesformat):
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
                            //fil = //the base64 pdf
                            filnavn = "LE--113-2.pdf"
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
            //Behandlingsvedlegg
            dokumentInfo = DokumentInfo().apply {
                when {
                    legeeklaering.forbeholdLegeerklaring.tilbakeholdInnhold.equals(2.toBigInteger()) -> {
                        begrensetPartsinnsynFraTredjePart = false
                    }

                    else -> begrensetPartsinnsynFraTredjePart = true

                }
                fildetaljerListe.add(Fildetaljer().apply {
                    //fil = //the base64 pdf
                    filnavn = "LE-behandlingsvedlegg-113-2.pdf"
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
            brukerId = "04030350265"
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

fun createPDFBase64Encoded(legeeklaering: Legeerklaring): String{

    val document = PDDocument()
    val blankPage = PDPage()
    document.addPage(blankPage)
    val font = PDType1Font.HELVETICA_BOLD;
    val contentStream = PDPageContentStream(document, blankPage)

    contentStream.beginText()
    val folketrygdenxPosision = 15f
    val folketrygdenyPosision = 750f
    contentStream.setFont(font, 18f)
    contentStream.newLineAtOffset(folketrygdenxPosision, folketrygdenyPosision)
    contentStream.showText("FOLKETRYGDEN")
    contentStream.endText()

    contentStream.beginText()
    val legeerklringVedxPosision = 350f
    val legeerklringVedyPosision = 760f
    contentStream.setFont(font, 12f)
    contentStream.newLineAtOffset(legeerklringVedxPosision, legeerklringVedyPosision)
    contentStream.showText("Legeerklæring ved arbeidsuførhet")
    contentStream.endText()

    contentStream.beginText()
    val legeesendeNAVKontorxPosision = 350f
    val legeesendeNAVKontoryPosision = 740f
    contentStream.setFont(font, 8f)
    contentStream.newLineAtOffset(legeesendeNAVKontorxPosision, legeesendeNAVKontoryPosision)
    contentStream.showText("Legen skal sende denne til NAV-kontoret.")
    contentStream.endText()

    contentStream.beginText()
    val erklaeringenGjelderxPosision = 15f
    val erklaeringenGjelderyPosision = 720f
    contentStream.setFont(font, 12f)
    contentStream.newLineAtOffset(erklaeringenGjelderxPosision, erklaeringenGjelderyPosision)
    contentStream.showText("0 Erklæringen gjelder")
    contentStream.endText()

    contentStream.addRect(15f, 665f, 550f, 40f)
    contentStream.setNonStrokingColor(Color.white)

    contentStream.close()

    val byteArrayOutputStream = ByteArrayOutputStream()
    document.save(byteArrayOutputStream)


    return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }
}