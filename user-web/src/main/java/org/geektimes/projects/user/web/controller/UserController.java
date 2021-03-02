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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 注册
 */
@Path("/user")
public class UserController implements PageController {

    @Override
    @GET
    @Path("/register")
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {

        String name = request.getParameter("name");
        // 1.register page
        if(StringUtils.isEmpty(name)) {
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
        if(userService.register(user)) {
            return "success.jsp";
        }
        return "fail.jsp";
    }

}
