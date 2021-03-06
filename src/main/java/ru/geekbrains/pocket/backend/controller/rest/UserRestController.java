package ru.geekbrains.pocket.backend.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.geekbrains.pocket.backend.domain.Security.User;
import ru.geekbrains.pocket.backend.exception.UserNotFoundException;
import ru.geekbrains.pocket.backend.resource.UserResource;
import ru.geekbrains.pocket.backend.resource.UserResources;
import ru.geekbrains.pocket.backend.response.UsersErrorResponse;
import ru.geekbrains.pocket.backend.service.security.UserService;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "http://localhost:9000")
@Slf4j
public class UserRestController {
    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/id/{id}")
    public UserResource getUserById(@PathVariable ObjectId id) {
        User user = userService.getUserById(id);
        return new UserResource(user);
    }

    @GetMapping("/users/{name}")
    public UserResource getUserByName(@PathVariable String name) {
        return new UserResource(userService.getUserByUsername(name));
    }

    @GetMapping("/users2/{name}")
    public Resource<User> getUserByName2(@PathVariable String name) {
        User user = userService.getUserByUsername(name);
        return new Resource<>(user,
                linkTo(methodOn(UserRestController.class).getUserByName(name)).withSelfRel(),
                linkTo(methodOn(UserRestController.class).getAllUsers()).withRel("users"));
    }

    @GetMapping("/usersall")
    public UserResources getAllUsers3() {
        return new UserResources(userService.getAllUsers());
    }

    @GetMapping("/usersall2")
    public Resources<UserResource> getAllUsers2() {
        return new Resources<>(userService.getAllUserResources());
    }

    @GetMapping("/usersall3")
    public Resources<Resource<User>> getAllUsers() {
        List<Resource<User>> users = userService.getAllUsers().stream()
                .map(user -> new Resource<>(user,
                        linkTo(methodOn(UserRestController.class).getUserByName2(user.getUsername())).withSelfRel(),
                        linkTo(methodOn(UserRestController.class).getAllUsers()).withRel("users")))
                .collect(Collectors.toList());

        return new Resources<>(users,
                linkTo(methodOn(UserRestController.class).getAllUsers()).withSelfRel());
    }

    @GetMapping("/users/all/")
    public List<User> getAllUsers4() {
        return userService.getAllUsers();
    }

    @PostMapping("/users/")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        //TODO вывод ошибки если юзер уже есть или не указан
        if (user == null) return null;
        user.setId(null);
        user = userService.insert(user);
        HttpHeaders httpHeaders = new HttpHeaders();

        Link forOneBookmark = new UserResource(user).getLink("self");
        httpHeaders.setLocation(URI.create(forOneBookmark.getHref()));

        log.info("addUser: " + user);

        return new ResponseEntity<User>(null, httpHeaders, HttpStatus.CREATED);
    }

    @PutMapping("/users/")
    public User updateUser(Principal principal, @RequestBody User user) {
        //обновление доступно только текущего авторизованного юзера
        String username = principal.getName();
        user.setId(userService.validateUser(username).getId());
        log.info("update: " + user);
        return userService.update(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable ObjectId id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler
    public ResponseEntity<UsersErrorResponse> handleException(UserNotFoundException exc) {
        UsersErrorResponse usersErrorResponse = new UsersErrorResponse();
        usersErrorResponse.setStatus(HttpStatus.NOT_FOUND.value());
        usersErrorResponse.setMessage(exc.getMessage());
        usersErrorResponse.setTimestamp(System.currentTimeMillis());
        return new ResponseEntity<>(usersErrorResponse, HttpStatus.NOT_FOUND);
    }
}
