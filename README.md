# user-platform



### 已完成

- 注册页面
- controller-service-repository
- JNDI

### 问题

- 整体上无法串起来

- 通过java -jar运行打包程序，访问register页面OK，但是点击注册报错

  ``` java
  严重: Servlet.service() for servlet [FrontControllerServlet] in context with path [] threw exception [null] with root cause
  javax.servlet.ServletException
          at org.geektimes.context.FrontControllerServlet.service(FrontControllerServlet.java:163)
          at javax.servlet.http.HttpServlet.service(HttpServlet.java:728)
          at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:305)
          at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:210)
          at org.geektimes.projects.user.web.filter.CharsetEncodingFilter.doFilter(CharsetEncodingFilter.java:37)
  
  ```

  

### 过程

- 看视频比较难独自完成作业，需要参考其他同学
- 找文档很容易找到不严谨甚至错误的博客文档，试验过程也费时费力，希望后续提供参考文档链接。