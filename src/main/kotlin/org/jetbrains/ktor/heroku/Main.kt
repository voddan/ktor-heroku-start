package org.jetbrains.ktor.heroku

import com.zaxxer.hikari.*
import freemarker.cache.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.freemarker.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.netty.*
import org.jetbrains.ktor.routing.*
import java.util.*
import kotlinx.html.*
import kotlinx.html.dom.*
import kotlinx.html.stream.appendHTML
import org.jetbrains.ktor.logging.logInfo
import org.jetbrains.ktor.request.host
import org.jetbrains.ktor.request.userAgent
import org.jetbrains.ktor.response.contentType
import org.jetbrains.ktor.response.header
import java.io.File

val hikariConfig = HikariConfig().apply {
    jdbcUrl = System.getenv("JDBC_DATABASE_URL")
}

val dataSource = if (hikariConfig.jdbcUrl != null)
    HikariDataSource(hikariConfig)
else
    HikariDataSource()

val html_utf8 = ContentType.Text.Html.withCharset(Charsets.UTF_8)

var counter = 0;

fun Application.module() {
    install(DefaultHeaders)
    install(ConditionalHeaders)
    install(PartialContentSupport)

    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(environment.classLoader, "templates")
    }

    install(StatusPages) {
        exception<Exception> { exception ->
            call.respond(FreeMarkerContent("error.ftl", exception, "", html_utf8))
        }
    }

    install(Routing) {
        serveClasspathResources("public")

        get("/") {
            val html = StringBuilder().appendHTML(true).html {
                head {
                    title { +"title1" }
                }
                body {
                    +"body3"
                    p {
                        val from = call.request.queryParameters.get("from")
                        if(from != null) {
                            +("from: " + from)
                        }
                    }
                    p {
                        +("agent: " + call.request.userAgent())
                    }
                    p {
                        +("remoteHost: " + call.request.local.remoteHost)
                    }
                    p {
                        +("host: " + call.request.local.host)
                    }
                    p {
                        +("uri: " + call.request.local.uri)
                    }
                    p {
                        +("scheme: " + call.request.local.scheme)
                    }
                }
            }
            call.response.header("Content-Type", "text/html; charset=UTF-8")
            call.response.header("my_header", "my_value")
            call.response.status(HttpStatusCode.OK)
            val absolutePath = File(".").absolutePath
            val toString = File(".").listFiles().joinToString()
            val message = File("src/main/resources/public/test.txt").readText()
            call.respond(message + "\r\n" + "absolutePath = " + absolutePath + "\r\n" +
                    "toString = " + toString);
        }

//        get("/") {
//            val model = HashMap<String, Any>()
//            model.put("message", "Hello World!")
//            val etag = model.toString().hashCode().toString()
//            call.respond(FreeMarkerContent("index.ftl", model, etag, html_utf8))
//        }

        get("/db") {
            var result:String = ""
            val model = HashMap<String, Any>()
            dataSource.connection.use { connection ->
                val rs = connection.createStatement().run {
                    executeUpdate("DROP TABLE loads")
                    executeUpdate("CREATE TABLE IF NOT EXISTS loads (" +
                            "time timestamp," +
                            "commingFrom VARCHAR(20)," +
                            "remoteHost VARCHAR(20)" +
//                            "," +
//                            "agent TEXT" +
                            ")");
                    executeUpdate("INSERT INTO loads (time, commingFrom, remoteHost, agent) VALUES(" +
                            "now()," +
                            "'" + call.request.queryParameters.get("from") + "'," +
                            " '" + call.request.local.remoteHost +
//                            "'," +
//                            " " + call.request.userAgent() +
                            ")")
                    executeQuery("SELECT * FROM loads")
                }


                val output = ArrayList<String>()
                while (rs.next()) {
                    result += rs.toString();
//                    output.add("Read from DB: " + rs.toString())//getTimestamp("time"))
                }
//                model.put("results", output)
            }
            call.respond(result);
//            val etag = model.toString().hashCode().toString()
//            call.respond(FreeMarkerContent("db.ftl", model, etag, html_utf8))
        }
        get("/db2") {
            var result:String = ""
            val model = HashMap<String, Any>()
            dataSource.connection.use { connection ->
                val rs = connection.createStatement().run {
                    //executeUpdate("DROP TABLE IF EXISTS ticks")
                    executeUpdate("CREATE TABLE IF NOT EXISTS test (tick timestamp")
                    executeUpdate("INSERT INTO test VALUES (now())")
                    executeQuery("SELECT test FROM ticks")
                }
                while (rs.next()) {
                    result += rs.getString("tick")
                }
            }
            call.respond(result);
        }
    }
}

fun main(args: Array<String>) {
    var port:Int = 5000
    try{
        port = Integer.valueOf(System.getenv("PORT"))
    } catch(e:Exception) {

    }
    embeddedServer(Netty, port, reloadPackages = listOf("heroku"), module = Application::module).start()
//    embeddedServer(MyServer,port, reloadPackages = listOf("heroku"), module = Application::module).start()
}

object MyServer : ApplicationHostFactory<ApplicationHost> {
    override fun create(environment: ApplicationHostEnvironment): ApplicationHost {
        return MyServer.create(applicationHostEnvironment {

        })
    }

}


