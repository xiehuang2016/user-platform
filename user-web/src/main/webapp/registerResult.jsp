<head>
<jsp:directive.include
	file="/WEB-INF/jsp/prelude/include-head-meta.jspf" />
<title>Register success</title>
</head>
<body>
	<div class="container-lg">
		Register
		<%=request.getAttribute("registerResult")%>
	</div>
</body>