package com.community.waste.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui.html";
    }

    @GetMapping("/api/public/health")
    @org.springframework.web.bind.annotation.ResponseBody
    public Object health() {
        return java.util.Map.of("status", "UP", "service", "reno-waste", "version", "1.0.0");
    }
}
