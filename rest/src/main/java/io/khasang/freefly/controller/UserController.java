package io.khasang.freefly.controller;

import io.khasang.freefly.dto.UserDTO;
import io.khasang.freefly.dto.Util;
import io.khasang.freefly.entity.Role;
import io.khasang.freefly.entity.User;
import io.khasang.freefly.service.RoleService;
import io.khasang.freefly.service.UserService;
import io.khasang.freefly.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping(value = "/user")
public class UserController {

    //codes error. apply during check data for creation or updating user
    private final int NO_ERROR = 0;
    private final int NON_UNIQUE_LOGIN = 1;
    private final int NON_UNIQUE_EMAIL = 2;
    private final int NOT_VALID_EMAIL = 3;
    private final int UNDEFINITED_FIST_NAME = 4;
    private final int UNDEFINITED_SECOND_NAME = 5;
    private final int UNDEFINITED_LOGIN = 6;

    private final UserService userService;
    private final RoleService roleService;
    private final SecurityUtil securityUtil;

    @Autowired
    Environment environment;

    @Autowired
    Util utilDTO;

    @Autowired
    public UserController(UserService userService, RoleService roleService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.roleService = roleService;
        this.securityUtil = securityUtil;
    }

    /**
     * method for creation user.
     *
     * @param user for creation
     * @return created user. created user has user.password's value encodes by BCryptPasswordEncoder
     * throws IEA with reason's code in message
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public User addUser(@RequestBody User user) {
        int code = checkCorrectDataForCreation(user);
        if (code == NO_ERROR) {
            user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
            return userService.addUser(user);
        } else {
            throw new IllegalArgumentException("User can not be added. Code reason = " + code);
        }
    }

    /**
     * method for creation no locked user.
     *
     * @param user for creation
     * @return created user. created user has user.password's value encodes by BCryptPasswordEncoder and user.isLock = false
     * throws IEA with reason's code in message
     */
    @RequestMapping(value = "/add/nolocked", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public UserDTO addNotLockedUser(@RequestBody User user) {
        user.setLock(false);
        List<Role> roleByDefault = roleService.getRoleByName(environment.getRequiredProperty("defaultRoleForNewUser"));
        user.setRoleList(roleByDefault);
        return utilDTO.getUserDTO(addUser(user));
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    @ResponseBody
    public UserDTO getUserDTOById(@PathVariable(value = "id") String id) {
        return userService.getUserDTOById(Long.parseLong(id));
    }

    @RequestMapping(value = "/get/all", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    @ResponseBody
    public List<UserDTO> getAllUsers() {
        return userService.getAllUserDTO();
    }

    /**
     * method for renovation user
     *
     * @param user data for updating
     * @return updated user.
     * throws IEA with reason's code in message specific in method checkCorrectDataForCreation's doc
     */
    @RequestMapping(value = "/update", method = RequestMethod.PUT, produces = "application/json;charset=utf-8")
    @ResponseBody
    public User updateUser(@RequestBody User user) {
        int code = checkCorrectDataForUpdating(user);
        if (code == NO_ERROR) {
            return userService.updateUser(user);
        } else {
            throw new IllegalArgumentException("User can not be updated. Code reason = " + code);
        }
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE, produces = "application/json;charset=utf-8")
    @ResponseBody
    public User deleteUser(@PathVariable(name = "id") String id) {
        return userService.deleteUserById(Long.parseLong(id));
    }

    /**
     * check data about new user: unique login, e-mail, and non empty info
     *
     * @param newUser
     * @return NO_ERROR if data correct or error's code
     */
    @RequestMapping(value = "/check", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public Integer checkCorrectDataForCreation(@RequestBody User newUser) {
        if (Objects.nonNull(userService.getUserByLogin(newUser.getLogin()))) {
            return NON_UNIQUE_LOGIN;
        }
        if (Objects.nonNull(userService.getUserByEmail(newUser.getEmail()))) {
            return NON_UNIQUE_EMAIL;
        }
        if (!verificationEmail(newUser.getEmail())) {
            return NOT_VALID_EMAIL;
        }

        return checkNonEmptyFields(newUser);
    }

    /**
     * check data for updated user: unique login, e-mail, and non empty info
     *
     * @param updatedUser new data for user
     * @return NO_ERROR if data correct or error's code
     */
    @RequestMapping(value = "/rest/check/for/update/info", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public Integer checkCorrectDataForUpdating(@RequestBody User updatedUser) {
        //if exists user with specific id
        if (Objects.nonNull(userService.getUserById(updatedUser.getId()))) {

            User userSameLogin = userService.getUserByLogin(updatedUser.getLogin());
            if (Objects.nonNull(userSameLogin)) {
                //if user with the same login is not the user for update
                if (!userSameLogin.getId().equals(updatedUser.getId())) {
                    return NON_UNIQUE_LOGIN;
                }
            }

            User userSameEmail = userService.getUserByEmail(updatedUser.getEmail());
            if (Objects.nonNull(userSameEmail)) {
                //if user with the same email is not the user for update
                if (!userSameEmail.getId().equals(updatedUser.getId())) {
                    return NON_UNIQUE_EMAIL;
                }
            }

            if (!verificationEmail(updatedUser.getEmail())) {
                return NOT_VALID_EMAIL;
            }

            return checkNonEmptyFields(updatedUser);

        } else {
            //not exists user with specific id
            return NO_ERROR;
        }
    }

    private int checkNonEmptyFields(User user) {
        if (user.getFirstName().isEmpty()) {
            return UNDEFINITED_FIST_NAME;
        }
        if (user.getLastName().isEmpty()) {
            return UNDEFINITED_SECOND_NAME;
        }
        if (user.getLogin().isEmpty()) {
            return UNDEFINITED_LOGIN;
        }

        return NO_ERROR;
    }

    private boolean verificationEmail(String email) {
        return email.matches("(.+)@(.+)\\.(.+)");
    }

    /**
     * for getting info about
     * @return
     */
    @RequestMapping(value = "/rest/security/authentication/current", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    @ResponseBody
    public UserDTO getAboutAutorizedUser() {
        User userFromDB = securityUtil.getAuthorizedUser();
        UserDTO aboutUser = utilDTO.getUserDTO(userFromDB);
        return aboutUser;
    }

    /**
     * method for updating info about user
     * info for updating:
     * email, firstName, lastName
     *
     * @param user contains data for updating
     * @return updated user.
     * throws IEA with reason's code in message specific in method checkCorrectDataForCreation's doc
     */
    @RequestMapping(value = "/rest/update/info", method = RequestMethod.PUT, produces = "application/json;charset=utf-8")
    @ResponseBody
    public UserDTO updateInfoAboutUser(@RequestBody User user) {
        int code = checkCorrectDataForUpdating(user);
        if (code == NO_ERROR) {
            User userInDb = userService.getUserById(user.getId());
            userInDb.setLogin(user.getLogin());
            userInDb.setEmail(user.getEmail());
            userInDb.setFirstName(user.getFirstName());
            userInDb.setLastName(user.getLastName());
            userInDb = userService.updateUser(userInDb);
            return utilDTO.getUserDTO(userInDb);
        } else {
            throw new IllegalArgumentException("User can not be updated. Code reason = " + code);
        }
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @RequestMapping("/update/info")
    public String getUpdatingPage(){
        return "user/updating/info";
    }

    /**
     * method for updating login
     * @param aboutNewLogin container for data: user's id = aboutNewLogin.getId(), new user's login = aboutNewLogin.getLogin()
     * @return dto about user. Able throws IAE, in case not unique login
     */
    @RequestMapping(value = "/rest/update/login", method = RequestMethod.PUT, produces = "application/json;charset=utf-8")
    @ResponseBody
    public UserDTO updateLogin(@RequestBody UserDTO aboutNewLogin) {
        User userInDb = userService.getUserById(aboutNewLogin.getId());
        userInDb.setLogin(aboutNewLogin.getLogin());
        userInDb = updateUser(userInDb);
        return utilDTO.getUserDTO(userInDb);
    }

    /**
     * method for definition unique login
     * @param login
     * @return true, if not exists user with specific login, else false
     */
    @RequestMapping(value = "/rest/check/unique/login/{value}")
    @ResponseBody
    public boolean checkUniqueLogin(@PathVariable (value = "value") String login){
        return Objects.isNull(userService.getUserByLogin(login));
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @RequestMapping("/update/login")
    public String getUpdatingLoginPage(){
        return "user/updating/login";
    }


    @RequestMapping(value = "rest/check/password", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public boolean checkPasswordForValid(@RequestBody UserDTO userDTO){
        return securityUtil.checkPassword(userDTO.getLogin(), userDTO.getPassword());
    }

    /**
     * method for update authorized user's password
     * @param password  new password
     * @return
     * throw NPE, if no user authorized
     */
    @RequestMapping(value = "/rest/update/password", method = RequestMethod.PUT, produces = "application/json;charset=utf-8")
    @ResponseBody
    public UserDTO updatePassword(@RequestBody String password) {
        User currentUser = securityUtil.getAuthorizedUser();
        currentUser.setPassword(new BCryptPasswordEncoder().encode(password));
        currentUser = userService.updateUser(currentUser);
        return utilDTO.getUserDTO(currentUser);
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @RequestMapping("update/password")
    public String getUpdatingPasswordPage(){
        return "user/updating/password";
    }



    //----------------------------
    /**
     * method for testing code
     * not for production!!!
     */
    @RequestMapping("/test")
    public void test(){
        securityUtil.checkPassword("login2", "111111");
        securityUtil.checkPassword("login", "111111");
        securityUtil.checkPassword("login", "11111");
    }
}
