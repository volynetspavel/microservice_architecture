import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Resource Processor calls GET /resources/1 and gets back binary audio/mpeg content"

    request {
        method GET()
        url("/resources/1")
    }

    response {
        status OK()
        headers {
            header("Content-Type", "audio/mpeg")
            header("Content-Disposition", 'attachment; filename="resource_1.mp3"')
        }
        body(fileAsBytes("test-audio.mp3"))
    }
}