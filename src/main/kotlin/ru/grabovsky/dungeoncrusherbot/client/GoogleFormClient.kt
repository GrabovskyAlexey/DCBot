package ru.grabovsky.dungeoncrusherbot.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "googleFormClient", url = "https://docs.google.com/forms/d/e/1FAIpQLScy7M05qKKNPno9rctHzQnlhDfWIpTVlmotJR1C-umjRohkUA/formResponse")
interface GoogleFormClient {
    @PostMapping(consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun submitForm(
        @RequestBody formData: Map<String, String>,
    )
}