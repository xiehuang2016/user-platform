package org.geektimes.projects.user.web.controller;

import org.apache.commons.lang.StringUtils;
import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserServiceImpl;
import org.geektimes.web.mvc.controller.PageController;
import org.geektimes.web.mvc.controller.RestController;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 注册
 */
@Path("/user")
public class UserController implements PageController {

    @POST
    @Path("/register")
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ServletContext servletContext = request.getServletContext();
        servletContext.log("register start");
        String name = request.getParameter("name");
        // 1.register page
        if(StringUtils.isEmpty(name)) {
            servletContext.log("register page");
            return "register.jsp";
        }
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String phoneNumber = request.getParameter("phoneNumber");
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        // 2.register
        UserServiceImpl userService = new UserServiceImpl();
        servletContext.log("register method before");
        if(userService.register(user)) {
            servletContext.log("register method success");
            return "success.jsp";
        }
        servletContext.log("register method fail");
        return "fail.jsp";
    }

}
