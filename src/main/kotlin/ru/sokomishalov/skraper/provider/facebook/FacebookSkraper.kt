/**
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.skraper.provider.facebook

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperHttpClient
import ru.sokomishalov.skraper.client.DefaultBlockingHttpClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.Post
import java.util.*
import java.util.UUID.randomUUID


/**
 * @author sokomishalov
 */
class FacebookSkraper @JvmOverloads constructor(
        override val client: SkraperHttpClient = DefaultBlockingHttpClient()
) : Skraper {

    companion object {
        private const val FACEBOOK_BASE_URL = "https://www.facebook.com"
        private const val FACEBOOK_GRAPH_BASE_URL = "http://graph.facebook.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val webPage = client.fetchDocument("$FACEBOOK_BASE_URL/${uri}/posts")
        val elements = webPage?.getElementsByClass("userContentWrapper")?.take(limit).orEmpty()

        return elements.map {
            Post(
                    id = it.getIdByUserContentWrapper(),
                    caption = it.getCaptionByUserContentWrapper(),
                    publishDate = it.getPublishedAtByUserContentWrapper(),
                    attachments = it.getAttachmentsByUserContentWrapper()
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String): String? {
        return "$FACEBOOK_GRAPH_BASE_URL/${uri}/picture?type=small"
    }

    private fun Element.getIdByUserContentWrapper(): String {
        return getElementsByAttributeValueContaining("id", "feed_subtitle")
                ?.first()
                ?.attr("id")
                ?: randomUUID().toString()
    }

    private fun Element.getCaptionByUserContentWrapper(): String? {
        return getElementsByClass("userContent")
                ?.first()
                ?.getElementsByTag("p")
                ?.first()
                ?.ownText()
                ?.toString()
    }

    private fun Element.getPublishedAtByUserContentWrapper(): Date {
        return getElementsByAttribute("data-utime")
                ?.first()
                ?.attr("data-utime")
                ?.let { Date(it.toLong().times(1000)) }
                ?: Date(0)
    }

    private fun Element.getAttachmentsByUserContentWrapper(): List<Attachment> {
        return getElementsByClass("scaledImageFitWidth")
                ?.first()
                ?.attr("src")
                ?.let { listOf(Attachment(url = it, type = IMAGE)) }
                ?: emptyList()
    }
}