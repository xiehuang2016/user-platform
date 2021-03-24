package org.geektimes.projects.user.web.controller;

import org.apache.commons.collections.CollectionUtils;
import org.geektimes.context.ComponentContext;
import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserServiceImpl;
import org.geektimes.web.mvc.controller.PageController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import java.util.Set;
import java.util.stream.Collectors;

import static org.geektimes.context.ComponentContext.CONTEXT_NAME;

/**
 * 注册接口
 */
@Path("/doUser")
public class DoUserController implements PageController {

    // xml cannot inject but spi inject ok
    @Resource(name = "bean/UserService")
    public UserServiceImpl userService;

    @Resource(name = "bean/Validator")
    private Validator validator;

    @PostConstruct
    public void init() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Override
    @POST
    @Path("/create")
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        ServletContext servletContext = request.getServletContext();
        servletContext.log("register start");
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        String email = request.getParameter("email");
        String phoneNumber = request.getParameter("phoneNumber");
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if(!CollectionUtils.isEmpty(violations)) {
            StringBuilder error = new StringBuilder("");
            violations.forEach(c -> {
                error.append(c.getMessage()).append(";");
            });
            request.setAttribute("registerResult", "fail, error message: " + error);
            return "registerResult.jsp";
            
        }
        // 2.register
        servletContext.log("register method before");
        servletContext.log("userService == null : " + (userService == null));
        // 注解注入失败，则手动注入
        if(userService == null) {
            // not good code
            servletContext.log("@Resource inject fail");
            ServletContext context = request.getServletContext();
            ComponentContext conponetContext = (ComponentContext) context.getAttribute(CONTEXT_NAME);
            userService = conponetContext.getComponent("bean/UserService");
        }
        servletContext.log("userService == null : " + (userService == null));
        if(userService.register(user)) {
            servletContext.log("register method success");
            request.setAttribute("registerResult", "success");
        } else {
            servletContext.log("register method fail");
            request.setAttribute("registerResult", "fail, error message: ");
        }
        return "registerResult.jsp";
    }

    public void setUserService(UserServiceImpl userService) {
        this.userService = userService;
    }
}
