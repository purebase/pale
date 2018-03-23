package no.nav.legeerklaering

interface Outcome {
}

class Success : Outcome {

}

class ValidationError(val message: String) : Outcome {
}