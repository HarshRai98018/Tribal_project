package com.klu.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/invoice"})
    public String index() {
        return "forward:/index.html";
    }
}
