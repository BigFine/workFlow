package com.example.service.util;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class MyJavaDelegate implements JavaDelegate {

	@Override
	public void execute(DelegateExecution execution) {
		System.out.println("这是自动执行的任务");
	}

}
