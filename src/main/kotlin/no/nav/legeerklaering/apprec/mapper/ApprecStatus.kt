package no.nav.legeerklaering.apprec.mapper


enum class ApprecStatus(val v: String, val dn: String){
    avvist("2","Avvist"),
    ok("1","OK")
}