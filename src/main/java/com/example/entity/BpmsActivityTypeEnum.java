package com.example.entity;

public enum BpmsActivityTypeEnum {
    
    START_EVENT("startEvent", "��ʼ�¼�"),
    END_EVENT("endEvent", "�����¼�"),
    USER_TASK("userTask", "�û�����"),
    EXCLUSIVE_GATEWAY("exclusiveGateway", "��������"),
    PARALLEL_GATEWAY("parallelGateway", "��������"),
    INCLUSIVE_GATEWAY("inclusiveGateway", "��������");
    
    private String type;
    private String name;
    
    private BpmsActivityTypeEnum(String type, String name) {
        this.type = type;
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
