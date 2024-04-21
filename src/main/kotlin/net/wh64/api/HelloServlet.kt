package net.wh64.api

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "helloServlet", value = ["/hello-servlet"])
class HelloServlet : HttpServlet() {
    private lateinit var message: String

    override fun init() {
        message = "Hello World!"
    }

    public override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        response.contentType = "text/html"

        val out = response.writer
        out.println("<html><body>")
        out.println("<h1>$message</h1>")
        out.println("</body></html>")
    }

    override fun destroy() {
    }
}