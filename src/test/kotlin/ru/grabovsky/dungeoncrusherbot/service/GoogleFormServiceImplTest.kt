package ru.grabovsky.dungeoncrusherbot.service

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.mockk
import io.mockk.verify
import ru.grabovsky.dungeoncrusherbot.client.GoogleFormClient

class GoogleFormServiceImplTest : ShouldSpec({
    val client = mockk<GoogleFormClient>(relaxed = true)
    val service = GoogleFormServiceImpl(client)

    should("отправлять данные в Google Form") {
        service.sendDraadorCount("15", "tester#1234")

        verify {
            client.submitForm(match {
                it["entry.449666117"] == "15" && it["entry.1851349150"] == "tester#1234"
            })
        }
    }
})
