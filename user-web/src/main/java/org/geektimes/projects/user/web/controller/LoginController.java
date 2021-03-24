package org.geektimes.projects.user.web.controller;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.web.mvc.controller.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.Collection;
import java.util.Map;

/**
 * 登录Controller
 * @author jizhi7
 * @since 1.0
 **/
@Path("/user")
public class LoginController implements RestController {

    @Resource(name = "bean/UserService")
    private UserService userService;

    @Path("/login")
    @POST
    public Collection<User> login() {
        Collection<User> users = userService.queryAll();
        return users;
    }

    @Path("/all")
    @POST
    public Collection<User> all() {
        Collection<User> users = userService.queryAll();
        return users;
    }

    @Path("/register")
    @POST
    public String register(HttpServletRequest request, @Valid User user, Map<String, String> error) {
        if(error != null && error.size() > 0) {
            request.setAttribute("error", error);
            return "/register-form.jsp";
        }
        if (userService.register(user)) {
            return "/register-success.jsp";
        }
        return "/register-form.jsp";
    }

    @Path("/test")
    @GET
    public String test() {
        userService.test();
        return "ok";
    }

}
