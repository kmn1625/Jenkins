package com.example.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class CalculatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);
    }
}
    
    @PostMapping("/calculate")
    @ResponseBody
    public Result calculate(@RequestParam double num1, 
                          @RequestParam double num2, 
                          @RequestParam String operation) {
        double result = 0;
        String error = null;
        
        try {
            switch(operation) {
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
                    if(num2 == 0) {
                        error = "Cannot divide by zero";
                    } else {
                        result = num1 / num2;
                    }
                    break;
                default:
                    error = "Invalid operation";
            }
        } catch(Exception e) {
            error = e.getMessage();
        }
        
        return new Result(result, error);
    }
}

class Result {
    private double result;
    private String error;
    
    public Result(double result, String error) {
        this.result = result;
        this.error = error;
    }
    
    public double getResult() { return result; }
    public void setResult(double result) { this.result = result; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
