<head>
<jsp:directive.include
	file="/WEB-INF/jsp/prelude/include-head-meta.jspf" />
<title>用户注册</title>
</head>
<body>
	<div class="container-lg">
		用户注册
	</div>
	<div>
		<form action="${pageContext.request.contextPath}/doUser/create" method="post" >
			<label>姓名</label><input type="text" required="true" name="name"/>
			<label>密码</label><input type="password" required="true" name="password"/>
			<label>邮箱</label><input type="text" required="true" name="email"/>
			<label>手机号</label><input type="text" name="phoneNumber"/>
			<input type="submit" value="立即注册">
		</form>
	</div>
</body>