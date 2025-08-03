package com.projectmanagement.dto;

import com.projectmanagement.model.TaskColumn;

public class TaskColumnDTO {
    private Long id;
    private String name;
    private String description;
    private Integer order;
    private String color;
    private Boolean isDefault;
    private Long projectId;
    private String projectName;
    private Long createdById;
    private String createdByName;

    // Constructors
    public TaskColumnDTO() {}

    public TaskColumnDTO(TaskColumn column) {
        this.id = column.getId();
        this.name = column.getName();
        this.description = column.getDescription();
        this.order = column.getOrder();
        this.color = column.getColor();
        this.isDefault = column.getIsDefault();
        
        if (column.getProject() != null) {
            this.projectId = column.getProject().getId();
            this.projectName = column.getProject().getName();
        }
        
        if (column.getCreatedBy() != null) {
            this.createdById = column.getCreatedBy().getId();
            this.createdByName = column.getCreatedBy().getFullName();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}
