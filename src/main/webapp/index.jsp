<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />

        <link rel="icon" href="${pageContext.request.contextPath}/public/favicon.ico" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/public/css/globals.css"/>

        <title>Redirecting...</title>
    </head>
    <body>
        <h1>Redirect to API's Repository...</h1>
        <% response.sendRedirect("https://github.com/wh64dev/wh64-api"); %>
    </body>
</html>