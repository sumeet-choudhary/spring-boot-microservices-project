package com.employee.events;

public class EmployeeEvent {

    private String eventId;
    private String eventType;
    private Long empId;
    private String empName;
    private String empEmail;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getEmpEmail() {
        return empEmail;
    }

    public void setEmpEmail(String empEmail) {
        this.empEmail = empEmail;
    }

    public EmployeeEvent() {
    }

    public EmployeeEvent(String eventId, String eventType, Long empId, String empName, String empEmail) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.empId = empId;
        this.empName = empName;
        this.empEmail = empEmail;
    }
}
