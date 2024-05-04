<%--
  Created by IntelliJ IDEA.
  User: wh64
  Date: 5/4/24
  Time: 2:43 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page import="net.wh64.api.Config" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta charset="UTF-8" />
    <meta http-equiv="X-UA-compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />

    <link rel="icon" href="${pageContext.request.contextPath}/public/favicon.ico" />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/public/css/globals.css"/>

    <title>Not Found</title>
</head>
<body>
    <h1>
        <code>404 Not Found</code>
    </h1>
    <p>만약 계속 문제가 발생한다면 <a href="mailto:me@projecttl.net">me@projecttl.net</a>으로 연락 주시기 바랍니다.</p>

    <hr />
    <pre>API Version: <%= Config.INSTANCE.getVersion() %></pre>
</body>
</html>
