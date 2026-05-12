import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Resource Processor calls POST /songs with a valid SongCreateRequestDto and gets back the song ID"

    request {
        method POST()
        url("/songs")
        headers {
            contentType(applicationJson())
        }
        body([
            id      : 5,
            name    : "Test Song",
            artist  : "Test Artist",
            album   : "Test Album",
            duration: "03:45",
            year    : "2020"
        ])
        bodyMatchers {
            jsonPath('$.id',       byRegex('[1-9][0-9]*'))
            jsonPath('$.name',     byRegex('.{1,100}'))
            jsonPath('$.artist',   byRegex('.{1,100}'))
            jsonPath('$.album',    byRegex('.{1,100}'))
            jsonPath('$.duration', byRegex('\\d{2}:[0-5]\\d'))
            jsonPath('$.year',     byRegex('\\d{4}'))
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([id: 5])
        bodyMatchers {
            jsonPath('$.id', byType())
        }
    }
}