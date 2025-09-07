package dev.a301.stock.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users/auth")
public class AuthController {
  @GetMapping("/google") public String google() { return "redirect:/oauth2/authorization/google"; }
  @GetMapping("/kakao")  public String kakao()  { return "redirect:/oauth2/authorization/kakao"; }
}