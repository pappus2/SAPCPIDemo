import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def body = message.getBody(String)

    try {
        // Step 1: Parse JSON
        def jsonSlurper = new JsonSlurper()
        def inputJson = jsonSlurper.parseText(body)

        // Step 2: Logging original JSON (if logging is available)
        if (this.binding.hasVariable("messageLog") && messageLog != null) {
            messageLog.addAttachmentAsString("Original JSON", body, "application/json")
            messageLog.setStringProperty("LogLevel", "Processing JSON")
        }

        // Step 3: Transform JSON
        def outputJson = [
            transactionId: inputJson.orderId,
            customer: [
                fullName: "${inputJson.customer.firstName} ${inputJson.customer.lastName}".trim(),
                email: inputJson.customer.email?.toLowerCase()
            ],
            totalAmount: inputJson.subtotal * (1 + (inputJson.taxRate ?: 0.1)),
            processedAt: new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX"),
            sourceSystem: "CPI"
        ]

        def outputStr = JsonOutput.prettyPrint(JsonOutput.toJson(outputJson))
        message.setBody(outputStr)
        message.setHeader("Content-Type", "application/json")
        message.setHeader("PayloadModified", "true")

        // Step 4: Log modified JSON
        if (this.binding.hasVariable("messageLog") && messageLog != null) {
            messageLog.addAttachmentAsString("Modified JSON", outputStr, "application/json")
        }

    } catch (Exception e) {
        // Error Logging
        if (this.binding.hasVariable("messageLog") && messageLog != null) {
            messageLog.addAttachmentAsString("Error", e.toString(), "text/plain")
        }

        message.setHeader("isError", "true")
        message.setBody("""{"error":"${e.message}"}""")
    }

    return message
}
