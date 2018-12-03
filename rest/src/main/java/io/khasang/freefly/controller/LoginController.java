package io.khasang.freefly.controller;

import io.khasang.freefly.service.UserService;
import io.khasang.freefly.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class LoginController {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    @Autowired
    public LoginController(UserService userService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.securityUtil = securityUtil;
    }

    /**
     * Home page
     */
    @RequestMapping("/")
    public ModelAndView getHelloPage() {
        ModelAndView model = new ModelAndView();
        if ("anonymousUser".equals(securityUtil.getAuthorizedUser())) {
            model.addObject("user", "Guest");
        } else {
            model.addObject("user", securityUtil.getAuthorizedUser());
        }
        model.setViewName("helloPage");
        return model;
    }

    /**
     * User login
     */
    @RequestMapping(value = {"/login"})
    public ModelAndView getLoginPage(@RequestParam(value = "error", required = false) String error) {
        ModelAndView model = new ModelAndView();
        if (error != null) {
            model.addObject("error", "Invalid username or password!");
        }
        model.setViewName("login");
        return model;
    }
}
