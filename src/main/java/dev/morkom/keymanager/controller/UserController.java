//package dev.morkom.keymanager.controller;
//
//import dev.morkom.keymanager.dto.CreateUserRequest;
//import dev.morkom.keymanager.dto.UpdatePasswordRequest;
//import dev.morkom.keymanager.dto.UpdateRolesRequest;
//import dev.morkom.keymanager.dto.UserDto;
//import dev.morkom.keymanager.service.UserService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/users")
//public class UserController {
//
//    private final UserService userService;
//
//    public UserController(UserService userService) {
//        this.userService = userService;
//    }
//
//    @GetMapping
//    public ResponseEntity<List<UserDto>> getAllUsers() {
//        return ResponseEntity.ok(userService.getAllUsers());
//    }
//
//    @PostMapping
//    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
//        return ResponseEntity.ok(userService.createUser(request));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
//        userService.deleteUser(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PutMapping("/{id}/password")
//    public ResponseEntity<Void> updateUserPassword(@PathVariable Long id, @RequestBody UpdatePasswordRequest request) {
//        userService.updateUserPassword(id, request);
//        return ResponseEntity.noContent().build();
//    }
//
//    @PutMapping("/{id}/roles")
//    public ResponseEntity<Void> updateUserRoles(@PathVariable Long id, @RequestBody UpdateRolesRequest request) {
//        userService.updateUserRoles(id, request);
//        return ResponseEntity.noContent().build();
//    }
//}
