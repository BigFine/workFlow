package com.example.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
public class TestController {
	@Autowired  
	private RepositoryService repositoryService; 
	//获取运行时服务组件
	@Autowired  
	private RuntimeService runtimeService;  
	//获取流程中的任务TASK组件
	@Autowired  
	private TaskService taskService;  
	@Autowired
	ProcessEngineFactoryBean processEngine;
	@Autowired
    HistoryService historyService;
	
	@RequestMapping("/demo1")
	public void demo1() {
		//部署流程
		repositoryService.createDeployment().addClasspathResource("myprocess.bpmn").deploy();
		//开启流程，myprocess是流程的ID 
		runtimeService.startProcessInstanceByKey("myProcess");
		
		//利用taskservice进行任务查询，查询第一个任务，查询后完成
        Task task=taskService.createTaskQuery().taskId("").singleResult();

        System.out.println("第一个流程任务完成前"+task.getName());

        taskService.complete(task.getId());
        
      //完成第一个任务后再次查询，出现第二个任务名称，完成第二个任务   
        task=taskService.createTaskQuery().singleResult();
        System.out.println("第二个流程任务完成前"+task.getName());
        taskService.complete(task.getId());
        
      //再次查询，TASK是NULL   
        task=taskService.createTaskQuery().singleResult();
        System.out.println("结束后"+task);
        
	}

	/**
	 * 获取流程图像，已执行节点和流程线高亮显示
	 */
	public void getActivitiProccessImage(String pProcessInstanceId, HttpServletResponse response)
	{
	    //logger.info("[开始]-获取流程图图像");
	    try {
	        //  获取历史流程实例
	        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
	                .processInstanceId(pProcessInstanceId).singleResult();

	        if (historicProcessInstance == null) {
	            //throw new BusinessException("获取流程实例ID[" + pProcessInstanceId + "]对应的历史流程实例失败！");
	        }
	        else
	        {
	            // 获取流程定义
	            ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(historicProcessInstance.getProcessDefinitionId());

	            // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
	            List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery()
	                    .processInstanceId(pProcessInstanceId).orderByHistoricActivityInstanceId().asc().list();

	            // 已执行的节点ID集合
	            List<String> executedActivityIdList = new ArrayList<String>();
	            int index = 1;
	            //logger.info("获取已经执行的节点ID");
	            for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
	                executedActivityIdList.add(activityInstance.getActivityId());

	                //logger.info("第[" + index + "]个已执行节点=" + activityInstance.getActivityId() + " : " +activityInstance.getActivityName());
	                index++;
	            }

	            BpmnModel bpmnModel = repositoryService.getBpmnModel(historicProcessInstance.getProcessDefinitionId());

	            // 已执行的线集合
	            List<String> flowIds = new ArrayList<String>();
	            // 获取流程走过的线 (getHighLightedFlows是下面的方法)
	            flowIds = getHighLightedFlows(bpmnModel,processDefinition, historicActivityInstanceList);



	            // 获取流程图图像字符流
	            ProcessDiagramGenerator pec = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
	            //配置字体
	            InputStream imageStream = pec.generateDiagram(bpmnModel, "png", executedActivityIdList, flowIds,"宋体","微软雅黑","黑体",null,2.0);

	            response.setContentType("image/png");
	            OutputStream os = response.getOutputStream();
	            int bytesRead = 0;
	            byte[] buffer = new byte[8192];
	            while ((bytesRead = imageStream.read(buffer, 0, 8192)) != -1) {
	                os.write(buffer, 0, bytesRead);
	            }
	            os.close();
	            imageStream.close();
	        }
	        //logger.info("[完成]-获取流程图图像");
	    } catch (Exception e) {
	        System.out.println(e.getMessage());
	        //logger.error("【异常】-获取流程图失败！" + e.getMessage());
	        //throw new BusinessException("获取流程图失败！" + e.getMessage());
	    }
	}

	public List<String> getHighLightedFlows(BpmnModel bpmnModel,ProcessDefinitionEntity processDefinitionEntity,List<HistoricActivityInstance> historicActivityInstances)
	{
	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //24小时制
	    List<String> highFlows = new ArrayList<String>();// 用以保存高亮的线flowId

	    for (int i = 0; i < historicActivityInstances.size() - 1; i++)
	    {
	        // 对历史流程节点进行遍历
	        // 得到节点定义的详细信息
	        FlowNode activityImpl = (FlowNode)bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(i).getActivityId());


	        List<FlowNode> sameStartTimeNodes = new ArrayList<FlowNode>();// 用以保存后续开始时间相同的节点
	        FlowNode sameActivityImpl1 = null;

	        HistoricActivityInstance activityImpl_ = historicActivityInstances.get(i);// 第一个节点
	        HistoricActivityInstance activityImp2_ ;

	        for(int k = i + 1 ; k <= historicActivityInstances.size() - 1; k++)
	        {
	            activityImp2_ = historicActivityInstances.get(k);// 后续第1个节点

	            if ( activityImpl_.getActivityType().equals("userTask") && activityImp2_.getActivityType().equals("userTask") &&
	                    df.format(activityImpl_.getStartTime()).equals(df.format(activityImp2_.getStartTime()))   ) //都是usertask，且主节点与后续节点的开始时间相同，说明不是真实的后继节点
	            {

	            }
	            else
	            {
	                sameActivityImpl1 = (FlowNode)bpmnModel.getMainProcess().getFlowElement(historicActivityInstances.get(k).getActivityId());//找到紧跟在后面的一个节点
	                break;
	            }

	        }
	        sameStartTimeNodes.add(sameActivityImpl1); // 将后面第一个节点放在时间相同节点的集合里
	        for (int j = i + 1; j < historicActivityInstances.size() - 1; j++)
	        {
	            HistoricActivityInstance activityImpl1 = historicActivityInstances.get(j);// 后续第一个节点
	            HistoricActivityInstance activityImpl2 = historicActivityInstances.get(j + 1);// 后续第二个节点

	            if (df.format(activityImpl1.getStartTime()).equals(df.format(activityImpl2.getStartTime()))  )
	            {// 如果第一个节点和第二个节点开始时间相同保存
	                FlowNode sameActivityImpl2 = (FlowNode)bpmnModel.getMainProcess().getFlowElement(activityImpl2.getActivityId());
	                sameStartTimeNodes.add(sameActivityImpl2);
	            }
	            else
	            {// 有不相同跳出循环
	                break;
	            }
	        }
	        List<SequenceFlow> pvmTransitions = activityImpl.getOutgoingFlows() ; // 取出节点的所有出去的线

	        for (SequenceFlow pvmTransition : pvmTransitions)
	        {// 对所有的线进行遍历
	            FlowNode pvmActivityImpl = (FlowNode)bpmnModel.getMainProcess().getFlowElement( pvmTransition.getTargetRef());// 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
	            if (sameStartTimeNodes.contains(pvmActivityImpl)) {
	                highFlows.add(pvmTransition.getId());
	            }
	        }

	    }
	    return highFlows;

	}
}
