package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Result<List<User>>> list() {
        return ResponseEntity.ok(Result.success(userService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<User>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(Result.success(userService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<Result<User>> create(@Valid @RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.created(userService.create(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<User>> update(@PathVariable Long id, @Valid @RequestBody User user) {
        return ResponseEntity.ok(Result.updated(userService.update(id, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(Result.deleted());
    }
}
