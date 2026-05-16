import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "resource-service publishes a ResourceUploadedEvent to the resource-uploaded exchange after an MP3 upload"

    input {
        // The generated verifier test calls this method on the base class
        triggeredBy("publishResourceUploadedEvent()")
    }

    outputMessage {
        sentTo("resource-uploaded")
        body([resourceId: 1])
        headers {
            header("contentType", "application/json")
        }
        bodyMatchers {
            jsonPath('$.resourceId', byRegex('[1-9][0-9]*'))
        }
    }
}