import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.util.Properties
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.find

abstract class UploadTask
    @Inject
    constructor(
        private val layout: ProjectLayout,
    ) : DefaultTask() {
        @get:Input
        abstract val botName: Property<String>

        @TaskAction
        fun upload() {
            val botName = botName.get()
            val sessionID = getSession()

            val jarFile =
                layout.buildDirectory
                    .file("libs/dynamiteBot.jar")
                    .get()
                    .asFile
            require(jarFile.exists()) { "Jar not found: ${jarFile.absolutePath}" }

            if (!uploadBot(botName, "POST", jarFile, sessionID)) {
                println("[Bot Exists] Bot with this name already exists. Use PUT to update it.")
                require(uploadBot(botName, "PUT", jarFile, sessionID)) {
                    "[Update Failed] Failed to update the bot. Please check your credentials and try again."
                }
            }
        }

        private fun getSession(): String {
            val properties = Properties()
            FileInputStream("local.properties").use { properties.load(it) }

            val url = URI("https://dynamite.softwire.com/api/login")
            val connection = url.toURL().openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:140.0) Gecko/20100101 Firefox/140.0",
            )
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Origin", "https://dynamite.softwire.com")

            val payload =
                """
                {"existingUserEmail":"${properties.getProperty("dynamite.email")}",
                "existingUserPassword":"${properties.getProperty("dynamite.password")}"}
                """.trimIndent()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            require(connection.responseCode == 204) {
                "[Login Failed] ${connection.responseCode}: ${connection.errorStream?.bufferedReader()?.readText()}"
            }

            connection.headerFields["Set-Cookie"]

            val cookies =
                connection.headerFields["Set-Cookie"]
                    ?: throw GradleException("[Cookie Not Found] No Set-Cookie header in login response")
            val sessionCookie =
                cookies.find { it.startsWith("connect.sid=") }
                    ?: throw GradleException("[Session Cookie Not Found] No connect.sid cookie in response")
            return sessionCookie.substringAfter("connect.sid=").substringBefore(';')
        }

        private fun uploadBot(
            botName: String,
            method: String,
            file: File,
            sessionID: String,
        ): Boolean {
            val boundary = "----GradleBoundary${UUID.randomUUID()}----"
            val uploadUrl = URI("https://dynamite.softwire.com/api/bots")
            val connection = uploadUrl.toURL().openConnection() as HttpURLConnection

            connection.requestMethod = method
            connection.doOutput = true
            connection.setRequestProperty("User-Agent", "GradleUploader/1.0")
            connection.setRequestProperty("Accept", "application/json, text/plain, */*")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Cookie", "connect.sid=$sessionID")

            with(connection.outputStream.bufferedWriter()) {
                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"botName\"\r\n\r\n")
                write("$botName\r\n")

                write("--$boundary\r\n")
                write("Content-Disposition: form-data; name=\"botFile\"; filename=\"${file.name}\"\r\n")
                write("Content-Type: application/java-archive\r\n\r\n")
                flush()
                Files.copy(file.toPath(), connection.outputStream)
                connection.outputStream.flush()
                write("\r\n")
                write("--$boundary--\r\n")
                flush()
            }

            val result = connection.responseCode == if (method == "POST") 200 else 201
            if (!result) {
                println("[Upload Failed] ${connection.responseCode}: ${connection.errorStream?.bufferedReader()?.readText()}")
            }
            return result
        }
    }
