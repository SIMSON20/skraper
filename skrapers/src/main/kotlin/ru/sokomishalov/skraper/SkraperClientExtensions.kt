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
package ru.sokomishalov.skraper

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.image.getRemoteImageInfo
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import java.nio.charset.Charset
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */

suspend fun SkraperClient.fetchBytes(
        url: String,
        headers: Map<String, String> = emptyMap()
): ByteArray? {
    return runCatching { fetch(url = url, headers = headers) }.getOrNull()
}

suspend fun SkraperClient.fetchJson(
        url: String,
        headers: Map<String, String> = emptyMap()
): JsonNode? {
    return runCatching { fetch(url = url, headers = headers).aReadJsonNodes() }.getOrNull()
}

suspend fun SkraperClient.fetchDocument(
        url: String,
        headers: Map<String, String> = emptyMap(),
        charset: Charset = UTF_8
): Document? {
    return runCatching { fetch(url = url, headers = headers)?.run { withContext(IO) { Jsoup.parse(toString(charset)) } } }.getOrNull()
}

suspend fun SkraperClient.fetchAspectRatio(
        url: String,
        headers: Map<String, String> = emptyMap(),
        orElse: Double = DEFAULT_POSTS_ASPECT_RATIO
): Double {
    return runCatching { withContext(IO) { openStream(url = url, headers = headers)!!.getRemoteImageInfo().run { width.toDouble() / height.toDouble() } } }.getOrElse { orElse }

}