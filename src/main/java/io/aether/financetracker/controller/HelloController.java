package io.aether.financetracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    // Equivalente no Express: app.get('/hello', (req, res) => res.send('...'))
    @GetMapping
    public String sayHello() {
        return "Hello World! Bem-vindo ao AetherFinanceTracker 🚀";
    }
}