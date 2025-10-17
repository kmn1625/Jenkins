package com.example.calculator;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CalculatorController {

    @GetMapping("/")
    public String home() {
        return "calculator"; // This renders templates/calculator.html
    }

    @PostMapping("/calculate")
    @ResponseBody
    public Result calculate(
            @RequestParam double num1,
            @RequestParam double num2,
            @RequestParam String operation) {

        double result = 0;
        String error = null;

        try {
            switch (operation) {
                case "add":
                    result = num1 + num2;
                    break;
                case "subtract":
                    result = num1 - num2;
                    break;
                case "multiply":
                    result = num1 * num2;
                    break;
                case "divide":
                    if (num2 == 0) {
                        error = "Cannot divide by zero";
                    } else {
                        result = num1 / num2;
                    }
                    break;
                default:
                    error = "Invalid operation";
            }
        } catch (Exception e) {
            error = e.getMessage();
        }

        return new Result(result, error);
    }
}
