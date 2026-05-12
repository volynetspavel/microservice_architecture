import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Resource Service calls DELETE /songs?id=1 and gets back the deleted IDs"

    request {
        method DELETE()
        url("/songs") {
            queryParameters {
                parameter("id", "1")
            }
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([ids: [1]])
        bodyMatchers {
            jsonPath('$.ids', byType {
                minOccurrence(0)
            })
        }
    }
}