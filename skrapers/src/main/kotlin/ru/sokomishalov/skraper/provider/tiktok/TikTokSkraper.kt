package ru.sokomishalov.skraper.provider.tiktok

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.consts.DEFAULT_USER_AGENT
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import ru.sokomishalov.skraper.model.MediaSize.*
import java.time.Duration


class TikTokSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://tiktok.com",
        private val apiBaseUrl: String = "https://m.tiktok.com/api",
        private val signer: TiktokSigner = DefaultTiktokSigner
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val userData = getUser(path = path)

        val secUid = userData?.getString("secUid").orEmpty()
        val userId = userData?.getString("userId").orEmpty()

        val url = apiBaseUrl.buildFullURL(
                path = "/item_list",
                queryParams = mapOf(
                        "secUid" to secUid,
                        "id" to userId,
                        "count" to limit,
                        "minCursor" to 0,
                        "maxCursor" to 0,
                        "lang" to "en",
                        "region" to "US",
                        "appId" to "1233",
                        "sourceType" to 8,
                        "type" to 1
                )
        )

        val signedUrl = with(signer) { client.sign(url = url, metadata = userData) }

        val data = client.fetchJson(
                url = signedUrl,
                headers = mapOf(
                        "Referer" to "$baseUrl${path}",
                        "Origin" to baseUrl,
                        "User-Agent" to DEFAULT_USER_AGENT
                )
        )

        val items = data
                ?.getByPath("body.itemListData")
                ?.mapNotNull { it.getByPath("itemInfos") }
                ?.toList()
                .orEmpty()

        return items.map { item ->
            with(item) {
                Post(
                        id = getString("id").orEmpty(),
                        text = getString("text"),
                        rating = getInt("diggCount"),
                        commentsCount = getInt("commentCount"),
                        viewsCount = getInt("playCount"),
                        media = getByPath("video")?.run {
                            listOf(Video(
                                    url = get("urls")?.firstOrNull()?.asText().orEmpty(),
                                    aspectRatio = getDouble("videoMeta.width") / getDouble("videoMeta.height"),
                                    duration = getLong("videoMeta.duration")?.let { sec -> Duration.ofSeconds(sec) }
                            ))
                        }.orEmpty()
                )
            }
        }
    }

    override suspend fun resolve(media: Media): Media {
        return media
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val user = getUser(path = path)

        return user?.run {
            PageInfo(
                    nick = getString("uniqueId").orEmpty(),
                    name = getString("nickName"),
                    description = getString("signature"),
                    followersCount = getInt("fans"),
                    avatarsMap = mapOf(
                            SMALL to user.getFirstAvatar("covers").toImage(),
                            MEDIUM to user.getFirstAvatar("coversMedium", "covers").toImage(),
                            LARGE to user.getFirstAvatar("coversLarge", "coversMedium", "covers").toImage()
                    )
            )
        }
    }


    private suspend fun getUser(path: String): JsonNode? {
        val document = client.fetchDocument(
                url = "${baseUrl}${path}",
                headers = mapOf("User-Agent" to DEFAULT_USER_AGENT)
        )

        val json = document
                ?.getElementById("__NEXT_DATA__")
                ?.html()
                ?.readJsonNodes()

        return json?.getByPath("props.pageProps.userData").apply {

            val tac = document
                    ?.getElementsByTag("script")
                    ?.firstOrNull { it.html().startsWith("tac=") }
                    ?.html()
                    ?.removeSurrounding("tac='", "'")

            tac?.let { (this as? ObjectNode)?.put("tac", it) }
        }
    }

    private fun JsonNode?.getFirstAvatar(vararg names: String): String {
        return names
                .mapNotNull {
                    this
                            ?.get(it)
                            ?.firstOrNull()
                            ?.asText()
                }
                .firstOrNull()
                .orEmpty()
    }
}