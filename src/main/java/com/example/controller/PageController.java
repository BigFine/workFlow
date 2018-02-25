package com.example.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/page")
public class PageController {

	@RequestMapping(value = "/demo1")
	public ModelAndView PageDemo() {
		ModelAndView mv = new ModelAndView("/webapp/page/PageDemo.jsp");
		return mv;
	}
}
