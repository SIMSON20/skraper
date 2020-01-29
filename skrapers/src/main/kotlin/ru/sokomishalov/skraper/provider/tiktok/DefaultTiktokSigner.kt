package ru.sokomishalov.skraper.provider.tiktok

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.model.URLString


/**
 * Default implementation for tiktok signature generator
 */
object DefaultTiktokSigner : TiktokSigner {
    override suspend fun SkraperClient.sign(url: URLString, metadata: JsonNode?): URLString {
        val signature = "n8-4gAAgEBAnfAa1UkRDL5.P-ZAAMEd" // todo
        val verifyFp = "verify_kb1fb81s_VMXJ7vcj_SitV_4NvW_BM1h_kQs5FFRI0BSx"

        return "${url}&_signature=${signature}&verifyFp=${verifyFp}"
    }
}